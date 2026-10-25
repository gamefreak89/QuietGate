package org.open.quietgate;

import android.content.Context;
import android.net.Uri;
import org.json.*;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Manual, all-or-nothing fetch. The caller identifier is never sent to Google. */
final class GoogleContactsSync {
    static final String SCOPE="https://www.googleapis.com/auth/contacts.readonly";
    static final class Unauthorized extends IOException {}
    static Set<String> fetch(Context c,String account,String generation,String token)throws Exception {
        Set<String> hashes=new HashSet<>(),seenPages=new HashSet<>();String page="";long total=0;
        for(int pages=0;pages<100;pages++) {
            if(!GoogleContactStore.active(c,account,generation))throw new IOException("Connection removed");
            Uri.Builder uri=Uri.parse("https://people.googleapis.com/v1/people/me/connections").buildUpon()
                .appendQueryParameter("personFields","emailAddresses,phoneNumbers")
                .appendQueryParameter("sources","READ_SOURCE_TYPE_CONTACT").appendQueryParameter("pageSize","1000");
            if(!page.isEmpty())uri.appendQueryParameter("pageToken",page);
            HttpsURLConnection connection=(HttpsURLConnection)new URL(uri.build().toString()).openConnection();
            connection.setConnectTimeout(15000);connection.setReadTimeout(20000);connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Authorization","Bearer "+token);connection.setRequestProperty("Accept","application/json");
            try {
                int code=connection.getResponseCode();if(code==401)throw new Unauthorized();if(code!=200)throw new IOException("Google contacts request failed");
                byte[] bytes;
                try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                    byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){total+=n;if(out.size()+n>2*1024*1024 || total>20*1024*1024)throw new IOException("Contacts response too large");out.write(b,0,n);}bytes=out.toByteArray();
                }
                JSONObject response=new JSONObject(new String(bytes,StandardCharsets.UTF_8));JSONArray people=response.optJSONArray("connections");
                if(people!=null)for(int i=0;i<people.length();i++){
                    JSONObject person=people.getJSONObject(i);
                    for(String field:new String[]{"emailAddresses","phoneNumbers"}) {
                        JSONArray values=person.optJSONArray(field);if(values==null)continue;
                        for(int j=0;j<values.length();j++){
                            JSONObject v=values.getJSONObject(j);add(hashes,v.optString("value"));
                            if("phoneNumbers".equals(field))add(hashes,v.optString("canonicalForm"));
                        }
                    }
                }
                page=response.optString("nextPageToken");if(page.isEmpty())return hashes;
                if(page.length()>8192 || !seenPages.add(page))throw new IOException("Invalid page token");
            }finally{connection.disconnect();}
        }
        throw new IOException("Contact list exceeds supported size; no partial list activated");
    }
    private static void add(Set<String> hashes,String value)throws Exception {
        String hash=GoogleContactStore.fingerprint(value);if(hash!=null)hashes.add(hash);
        if(hashes.size()>GoogleContactStore.MAX_IDENTIFIERS)throw new IOException("Contact list exceeds supported size");
    }
}
