package org.open.quietgate;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.*;
/** Local organizational history; imported identities never affect call decisions. */
final class CallCatalog {
 static final String KEY="rejected_call_catalog";
 static final String[] CATEGORIES={"Uncategorized","Suspected spam","Suspected scam","Business / service","Known caller","Other"};
 private static final int MAX=500;
 private static final long RETENTION=90L*24*60*60*1000;
 private static void save(Context c,JSONArray a) { c.getSharedPreferences(LeaseManager.PREFS,0).edit().putString(KEY,a.toString()).apply(); }
 static synchronized JSONArray recent(Context c) {
  JSONArray kept=new JSONArray();
  try {
   JSONArray saved=new JSONArray(c.getSharedPreferences(LeaseManager.PREFS,0).getString(KEY,"[]"));
   long cutoff=System.currentTimeMillis()-RETENTION;
   for(int i=0;i<saved.length() && kept.length()<MAX;i++) {
    JSONObject e=saved.optJSONObject(i);
    if(e!=null && e.optLong("time")>=cutoff) {
     if(!e.has("id")) e.put("id",UUID.randomUUID().toString());
     if(!e.has("category")) e.put("category",CATEGORIES[0]);
     kept.put(e);
    }
   }
  } catch(JSONException ignored) {}
  save(c,kept); return kept;
 }
 static synchronized void add(Context c,String identity,boolean sent) {
  try {
   JSONArray old=recent(c), result=new JSONArray(); JSONObject e=new JSONObject();
   e.put("id",UUID.randomUUID().toString()); e.put("time",System.currentTimeMillis());
   e.put("identity",identity==null?"Unknown identity":identity.substring(0,Math.min(identity.length(),320)));
   e.put("source","Meet notification; identity not authenticated");
   e.put("action",sent?"Decline sent; outcome unconfirmed":"Decline failed");
   e.put("category",CATEGORIES[0]); e.put("notes",""); e.put("imported",false); result.put(e);
   for(int i=0;i<old.length() && result.length()<MAX;i++) result.put(old.get(i));
   save(c,result);
  } catch(JSONException ignored) {}
 }
 static synchronized void edit(Context c,String id,String category,String notes) {
  JSONArray a=recent(c);
  for(int i=0;i<a.length();i++) {
   JSONObject e=a.optJSONObject(i);
   if(e!=null && id.equals(e.optString("id"))) try { e.put("category",category); e.put("notes",notes); } catch(JSONException ignored) {}
  }
  save(c,a);
 }
 static synchronized String exportJson(Context c) throws JSONException {
  return new JSONObject().put("format","QuietGate-call-catalog").put("version",1)
   .put("exportedAt",System.currentTimeMillis()).put("records",recent(c)).toString(2);
 }
 private static String fingerprint(JSONObject e) {
  return e.optLong("time")+"|"+e.optString("identity").trim().toLowerCase(Locale.ROOT)+"|"+e.optString("action");
 }
 static synchronized String merge(Context c,String json) throws JSONException {
  Object parsed=new org.json.JSONTokener(json).nextValue(); JSONArray incoming;
  if(parsed instanceof JSONArray) incoming=(JSONArray)parsed;
  else if(parsed instanceof JSONObject) {
   JSONObject obj=(JSONObject)parsed;
   if(!"QuietGate-call-catalog".equals(obj.optString("format")) || obj.optInt("version")!=1) throw new JSONException("Unsupported format");
   incoming=obj.getJSONArray("records");
  } else throw new JSONException("Expected a catalog");
  if(incoming.length()>5000) throw new JSONException("Too many records");
  JSONArray current=recent(c); List<JSONObject> merged=new ArrayList<>();
  Set<String> ids=new HashSet<>(), fingerprints=new HashSet<>();
  for(int i=0;i<current.length();i++) { JSONObject e=current.getJSONObject(i); merged.add(e); ids.add(e.optString("id")); fingerprints.add(fingerprint(e)); }
  int added=0,duplicates=0,skipped=0; long now=System.currentTimeMillis();
  for(int i=0;i<incoming.length();i++) {
   JSONObject e=incoming.optJSONObject(i);
   if(e==null || !(e.opt("time") instanceof Number) || !(e.opt("identity") instanceof String) || !(e.opt("action") instanceof String)) { skipped++; continue; }
   long time=e.optLong("time"); String identity=e.optString("identity"),action=e.optString("action");
   if(time<now-RETENTION || time>now+300000 || identity.trim().isEmpty() || identity.length()>320 || action.length()>160) { skipped++; continue; }
   String id=e.optString("id");
   if(fingerprints.contains(fingerprint(e)) || (!id.isEmpty() && ids.contains(id))) { duplicates++; continue; }
   if(id.isEmpty() || id.length()>100) id=UUID.randomUUID().toString();
   String category=e.optString("category",CATEGORIES[0]);
   if(!Arrays.asList(CATEGORIES).contains(category)) category=CATEGORIES[0];
   String notes=e.optString("notes",""); if(notes.length()>1000) { skipped++; continue; }
   JSONObject clean=new JSONObject().put("id",id).put("time",time).put("identity",identity)
    .put("action",action).put("source","Imported catalog; claims not verified").put("category",category)
    .put("notes",notes).put("imported",true);
   merged.add(clean); ids.add(id); fingerprints.add(fingerprint(clean)); added++;
  }
  merged.sort((a,b)->Long.compare(b.optLong("time"),a.optLong("time")));
  JSONArray result=new JSONArray(); for(int i=0;i<Math.min(MAX,merged.size());i++) result.put(merged.get(i));
  save(c,result);
  return "Merged "+added+" new records; "+duplicates+" duplicates; "+skipped+" invalid, expired or future records skipped.\nKept "+result.length()+" newest records (500 maximum). Existing category edits win on duplicates.";
 }
 static synchronized void clear(Context c) { c.getSharedPreferences(LeaseManager.PREFS,0).edit().remove(KEY).apply(); }
}
