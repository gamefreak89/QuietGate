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
        final String scheduleKey,refreshState;
        final int jobId;
        final boolean autoRefresh,wifiOnly;
        final long lastAttempt;
        Entry(String a,String g,String s,long u,Set<String> h){this(a,g,s,u,h,UUID.randomUUID().toString(),100000+new java.security.SecureRandom().nextInt(500000000),false,true,0,"Off");}
        Entry(String a,String g,String s,long u,Set<String> h,String k,int id,boolean auto,boolean wifi,long attempt,String refresh){account=a;generation=g;status=s;updated=u;hashes=Collections.unmodifiableSet(new HashSet<>(h));scheduleKey=k;jobId=id;autoRefresh=auto;wifiOnly=wifi;lastAttempt=attempt;refreshState=refresh;}
        Entry copy(String g,String s,long u,Set<String> h,long attempt,String refresh){return new Entry(account,g,s,u,h,scheduleKey,jobId,autoRefresh,wifiOnly,attempt,refresh);}
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
        Map<String,Entry> result=new LinkedHashMap<>();Set<Integer> jobIds=new HashSet<>();boolean migrate=false;
        for(int i=0;i<accounts.length();i++){
            JSONObject row=accounts.getJSONObject(i);JSONArray h=row.getJSONArray("hashes");if(h.length()>MAX_IDENTIFIERS)throw new IOException("Too many identifiers");
            Set<String> hashes=new HashSet<>();for(int j=0;j<h.length();j++)hashes.add(h.getString(j));
            String account=row.getString("account"),schedule=row.optString("scheduleKey","");int id=row.optInt("jobId",0);
            if(schedule.isEmpty()){schedule=UUID.randomUUID().toString();migrate=true;}
            if(id<100000 || id>=500100000 || jobIds.contains(id)){do{id=100000+new java.security.SecureRandom().nextInt(500000000);}while(jobIds.contains(id));migrate=true;}jobIds.add(id);
            result.put(account,new Entry(account,row.getString("generation"),row.getString("status"),row.getLong("updated"),hashes,schedule,id,row.optBoolean("autoRefresh",false),row.optBoolean("wifiOnly",true),row.optLong("lastAttempt",0),row.optString("refreshState","Off")));
        }
        state=result;if(migrate)save(c,result);return state;
    }
    private static void save(Context c,Map<String,Entry> next)throws Exception {
        JSONArray accounts=new JSONArray();
        for(Entry e:next.values())accounts.put(new JSONObject().put("account",e.account).put("generation",e.generation)
            .put("status",e.status).put("updated",e.updated).put("hashes",new JSONArray(e.hashes))
            .put("scheduleKey",e.scheduleKey).put("jobId",e.jobId).put("autoRefresh",e.autoRefresh).put("wifiOnly",e.wifiOnly)
            .put("lastAttempt",e.lastAttempt).put("refreshState",e.refreshState));
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
        if(old==null){old=new Entry(account,gen,"pending",0,Collections.emptySet());Set<Integer> used=new HashSet<>();for(Entry e:next.values())used.add(e.jobId);while(used.contains(old.jobId))old=new Entry(account,gen,"pending",0,Collections.emptySet());}
        // Refresh pauses matching until the new snapshot has completed in full.
        next.put(account,old.copy(gen,"pending",old.updated,Collections.emptySet(),System.currentTimeMillis(),"Manual refresh"));save(c,next);return gen;
    }
    static synchronized boolean active(Context c,String account,String generation)throws Exception {
        Entry e=load(c).get(account);return e!=null && e.generation.equals(generation);
    }
    static synchronized boolean complete(Context c,String account,String generation,Set<String> hashes)throws Exception {
        if(!active(c,account,generation))return false;
        if(hashes.size()>MAX_IDENTIFIERS)throw new IOException("Too many identifiers");
        Map<String,Entry> next=new LinkedHashMap<>(load(c));Entry old=next.get(account);next.put(account,old.copy(generation,"ready",System.currentTimeMillis(),hashes,old.lastAttempt,"Success"));save(c,next);return true;
    }
    static synchronized void failed(Context c,String account,String generation)throws Exception {
        if(!active(c,account,generation))return;
        Map<String,Entry> next=new LinkedHashMap<>(load(c));Entry old=next.get(account);
        next.put(account,old.copy(generation,"paused",old.updated,Collections.emptySet(),old.lastAttempt,"Manual refresh failed"));save(c,next);
    }
    static synchronized void remove(Context c,String account)throws Exception {
        Map<String,Entry> next=new LinkedHashMap<>(load(c));Entry removed=next.remove(account);save(c,next);if(removed!=null)GoogleAutoRefresh.cancel(c,removed);
    }
    static synchronized void clear(Context c)throws Exception {
        GoogleAutoRefresh.cancelAll(c);
        // Delete keys even when the ciphertext cannot be read; this also cryptographically erases orphaned cache bytes.
        KeyStore keys=KeyStore.getInstance("AndroidKeyStore");keys.load(null);keys.deleteEntry(ENCRYPT);keys.deleteEntry(HASH);
        file(c).delete();state=new LinkedHashMap<>();encryptionKey=null;hashKey=null;
    }
    static synchronized Entry configureAuto(Context c,String account,boolean enabled,boolean wifi)throws Exception {
        Map<String,Entry> next=new LinkedHashMap<>(load(c));Entry e=next.get(account);if(e==null)throw new IOException("Account unavailable");
        Entry changed=new Entry(account,UUID.randomUUID().toString(),e.status,e.updated,e.hashes,e.scheduleKey,e.jobId,enabled,wifi,e.lastAttempt,enabled?"Scheduled":"Off");
        next.put(account,changed);save(c,next);return changed;
    }
    static synchronized Entry bySchedule(Context c,String key)throws Exception {for(Entry e:load(c).values())if(e.scheduleKey.equals(key))return e;return null;}
    static synchronized String beginBackground(Context c,String schedule)throws Exception {
        Entry e=bySchedule(c,schedule);if(e==null || !e.autoRefresh || "pending".equals(e.status) || "Reconnect required".equals(e.refreshState))return null;
        if("Running".equals(e.refreshState) && System.currentTimeMillis()-e.lastAttempt>=0 && System.currentTimeMillis()-e.lastAttempt<10L*60*1000)return null;
        String gen=UUID.randomUUID().toString();Map<String,Entry> next=new LinkedHashMap<>(load(c));
        next.put(e.account,e.copy(gen,e.status,e.updated,e.hashes,System.currentTimeMillis(),"Running"));save(c,next);return gen;
    }
    static synchronized boolean completeBackground(Context c,String schedule,String gen,Set<String> hashes)throws Exception {
        Entry e=bySchedule(c,schedule);return e!=null && e.autoRefresh && e.generation.equals(gen) && complete(c,e.account,gen,hashes);
    }
    static synchronized boolean backgroundState(Context c,String schedule,String gen,String message,boolean consent)throws Exception {
        Entry e=bySchedule(c,schedule);if(e==null || !e.autoRefresh || !e.generation.equals(gen))return false;
        boolean notify=consent && !"Reconnect required".equals(e.refreshState);Map<String,Entry> next=new LinkedHashMap<>(load(c));
        next.put(e.account,e.copy(gen,consent?"paused":e.status,e.updated,consent?Collections.emptySet():e.hashes,e.lastAttempt,message));save(c,next);return notify;
    }
    static synchronized boolean requireReconnect(Context c,String schedule,String gen)throws Exception {
        Entry e=bySchedule(c,schedule);if(e==null || !e.autoRefresh || !e.generation.equals(gen))return false;
        boolean notify=backgroundState(c,schedule,gen,"Reconnect required",true);GoogleAutoRefresh.cancel(c,e);return notify;
    }
    static synchronized boolean retryBackground(Context c,String schedule,String gen)throws Exception {
        Entry e=bySchedule(c,schedule);if(e==null || !e.autoRefresh || !e.generation.equals(gen))return false;
        backgroundState(c,schedule,gen,"Retry pending",false);return true;
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
