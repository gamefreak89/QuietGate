package org.open.quietgate;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import android.util.Base64;
import org.json.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;

/** App-private encrypted snapshots. No tokens, raw contact lists, contact names or exports. */
final class GoogleContactStore {
    static final long FRESH_MS=24L*60*60*1000;
    static final int MAX_ACCOUNTS=10, MAX_IDENTIFIERS=20000;
    private static final String ENCRYPT="quietgate.google.saved.aes.v1", HASH="quietgate.google.saved.hmac.v1";
    private static Map<String,Entry> state;
    private static SecretKey encryptionKey,hashKey;
    static final class Entry {
        final String account,generation,status;
        final long updated;
        final Set<String> hashes;
        Entry(String a,String g,String s,long u,Set<String> h){account=a;generation=g;status=s;updated=u;hashes=Collections.unmodifiableSet(new HashSet<>(h));}
        boolean fresh(){long age=System.currentTimeMillis()-updated;return "ready".equals(status) && age>=0 && age<FRESH_MS;}
    }
    private static AtomicFile file(Context c){return new AtomicFile(new File(c.getNoBackupFilesDir(),"google-saved-contacts.enc"));}
    private static SecretKey key(String alias,boolean create)throws Exception {
        SecretKey cached=ENCRYPT.equals(alias)?encryptionKey:hashKey;if(cached!=null)return cached;
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(store.containsAlias(alias)){
            SecretKey found=(SecretKey)store.getKey(alias,null);if(ENCRYPT.equals(alias))encryptionKey=found;else hashKey=found;return found;
        }
        if(!create)throw new IOException("Contact cache key unavailable");
        boolean aes=ENCRYPT.equals(alias);
        KeyGenerator generator=KeyGenerator.getInstance(aes?KeyProperties.KEY_ALGORITHM_AES:KeyProperties.KEY_ALGORITHM_HMAC_SHA256,"AndroidKeyStore");
        KeyGenParameterSpec.Builder builder=new KeyGenParameterSpec.Builder(alias,aes
            ?KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT:KeyProperties.PURPOSE_SIGN|KeyProperties.PURPOSE_VERIFY);
        if(aes)builder.setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256);
        else builder.setDigests(KeyProperties.DIGEST_SHA256);
        generator.init(builder.build());SecretKey generated=generator.generateKey();if(aes)encryptionKey=generated;else hashKey=generated;return generated;
    }
    private static Map<String,Entry> load(Context c)throws Exception {
        if(state!=null)return state;
        if(!file(c).getBaseFile().exists()){state=new LinkedHashMap<>();return state;}
        byte[] encrypted;
        try(InputStream in=file(c).openRead();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>20*1024*1024)throw new IOException("Cache too large");out.write(b,0,n);}encrypted=out.toByteArray();
        }
        if(encrypted.length<29 || encrypted[0]!=1)throw new IOException("Unsupported contact cache");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(ENCRYPT,false),new GCMParameterSpec(128,Arrays.copyOfRange(encrypted,1,13)));
        JSONObject json=new JSONObject(new String(cipher.doFinal(encrypted,13,encrypted.length-13),StandardCharsets.UTF_8));
        if(json.getInt("version")!=1)throw new IOException("Unsupported contact cache");
        JSONArray accounts=json.getJSONArray("accounts");if(accounts.length()>MAX_ACCOUNTS)throw new IOException("Too many accounts");
        Map<String,Entry> result=new LinkedHashMap<>();
        for(int i=0;i<accounts.length();i++){
            JSONObject row=accounts.getJSONObject(i);JSONArray h=row.getJSONArray("hashes");if(h.length()>MAX_IDENTIFIERS)throw new IOException("Too many identifiers");
            Set<String> hashes=new HashSet<>();for(int j=0;j<h.length();j++)hashes.add(h.getString(j));
            String account=row.getString("account");result.put(account,new Entry(account,row.getString("generation"),row.getString("status"),row.getLong("updated"),hashes));
        }
        state=result;return state;
    }
    private static void save(Context c,Map<String,Entry> next)throws Exception {
        JSONArray accounts=new JSONArray();
        for(Entry e:next.values())accounts.put(new JSONObject().put("account",e.account).put("generation",e.generation)
            .put("status",e.status).put("updated",e.updated).put("hashes",new JSONArray(e.hashes)));
        byte[] bytes=new JSONObject().put("version",1).put("accounts",accounts).toString().getBytes(StandardCharsets.UTF_8);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(ENCRYPT,true));
        byte[] iv=cipher.getIV();if(iv.length!=12)throw new IOException("Invalid encryption IV");
        byte[] encrypted=cipher.doFinal(bytes);AtomicFile target=file(c);FileOutputStream out=null;
        try{out=target.startWrite();out.write(1);out.write(iv);out.write(encrypted);target.finishWrite(out);}
        catch(Exception e){if(out!=null)target.failWrite(out);throw e;}
        state=next;
    }
    static synchronized List<Entry> entries(Context c)throws Exception{return new ArrayList<>(load(c).values());}
    static synchronized String begin(Context c,String account)throws Exception {
        if(account==null || account.length()>320 || !account.contains("@"))throw new IOException("Invalid account");
        Map<String,Entry> next=new LinkedHashMap<>(load(c));
        if(!next.containsKey(account) && next.size()>=MAX_ACCOUNTS)throw new IOException("Maximum ten connected accounts");
        String gen=UUID.randomUUID().toString();Entry old=next.get(account);
        // Refresh pauses matching until the new snapshot has completed in full.
        next.put(account,new Entry(account,gen,"pending",old==null?0:old.updated,Collections.emptySet()));save(c,next);return gen;
    }
    static synchronized boolean active(Context c,String account,String generation)throws Exception {
        Entry e=load(c).get(account);return e!=null && e.generation.equals(generation);
    }
    static synchronized boolean complete(Context c,String account,String generation,Set<String> hashes)throws Exception {
        if(!active(c,account,generation))return false;
        if(hashes.size()>MAX_IDENTIFIERS)throw new IOException("Too many identifiers");
        Map<String,Entry> next=new LinkedHashMap<>(load(c));next.put(account,new Entry(account,generation,"ready",System.currentTimeMillis(),hashes));save(c,next);return true;
    }
    static synchronized void failed(Context c,String account,String generation)throws Exception {
        if(!active(c,account,generation))return;
        Map<String,Entry> next=new LinkedHashMap<>(load(c));Entry old=next.get(account);
        next.put(account,new Entry(account,generation,"paused",old.updated,Collections.emptySet()));save(c,next);
    }
    static synchronized void remove(Context c,String account)throws Exception {
        Map<String,Entry> next=new LinkedHashMap<>(load(c));next.remove(account);save(c,next);
    }
    static synchronized void clear(Context c)throws Exception {
        // Delete keys even when the ciphertext cannot be read; this also cryptographically erases orphaned cache bytes.
        KeyStore keys=KeyStore.getInstance("AndroidKeyStore");keys.load(null);keys.deleteEntry(ENCRYPT);keys.deleteEntry(HASH);
        file(c).delete();state=new LinkedHashMap<>();encryptionKey=null;hashKey=null;
    }
    static String normalized(String value) {
        if(value==null)return null;String v=value.trim();if(v.isEmpty() || v.length()>320)return null;
        if(v.contains("@"))return "e:"+v.toLowerCase(Locale.ROOT);
        StringBuilder b=new StringBuilder("p:");
        for(int i=0;i<v.length();i++){char ch=v.charAt(i);int digit=Character.digit(ch,10);if(digit>=0)b.append((char)('0'+digit));else if("+()- .".indexOf(ch)<0)return null;}
        return b.length()>2?b.toString():null;
    }
    static synchronized String fingerprint(String value)throws Exception {
        String normalized=normalized(value);if(normalized==null)return null;
        Mac mac=Mac.getInstance("HmacSHA256");mac.init(key(HASH,true));return Base64.encodeToString(mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
    }
    static synchronized boolean matches(Context c,String value)throws Exception {
        Map<String,Entry> accounts=load(c);boolean fresh=false;for(Entry e:accounts.values())if(e.fresh()){fresh=true;break;}if(!fresh)return false;
        String hash=fingerprint(value);if(hash==null)return false;
        for(Entry e:accounts.values())if(e.fresh() && e.hashes.contains(hash))return true;
        return false;
    }
}
