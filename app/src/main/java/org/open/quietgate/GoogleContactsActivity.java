package org.open.quietgate;

import android.accounts.*;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.google.android.gms.auth.api.identity.*;
import com.google.android.gms.common.api.*;
import java.util.*;

/** Each account is explicitly chosen and authorized. No silent account enumeration. */
public final class GoogleContactsActivity extends Activity {
    private static final int CHOOSE=61,AUTHORIZE=62;
    private String pendingAccount,pendingGeneration;
    private LinearLayout panel;
    private boolean working;
    private static final Set<String> REVOKING=Collections.synchronizedSet(new HashSet<>());
    private List<Scope> scopes(){return Collections.singletonList(new Scope(GoogleContactsSync.SCOPE));}
    @Override public void onCreate(Bundle saved){super.onCreate(saved);if(saved!=null){pendingAccount=saved.getString("account");pendingGeneration=saved.getString("generation");}render();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("account",pendingAccount);out.putString("generation",pendingGeneration);}
    @Override protected void onResume(){super.onResume();render();}
    private void render(){
        ScrollView scroll=new ScrollView(this);panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
        int p=(int)(20*getResources().getDisplayMetrics().density);panel.setPadding(p,p,p,p);scroll.addView(panel);setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});scroll.requestApplyInsets();
        text("Connected Google saved contacts",23);
        text("Optional. Device contacts still work without connecting Google. Choose each account yourself and approve read-only saved-contact access. QuietGate fetches only emails and phone numbers directly from Google when you connect or refresh; no contact names, Other contacts or Gmail access is requested.",15);
        text("Matching stays on this phone using an encrypted snapshot and keyed fingerprints. No caller identifier is sent to Google. No QuietGate server receives your contacts. Refresh is manual; matching expires 24 hours after the last successful refresh. Refresh failure pauses that account's matching. Changes or revocations made outside QuietGate may not be reflected until refresh or expiry.",15);
        button("Connect a Google account",()->new AlertDialog.Builder(this).setTitle("Connect saved Google contacts?")
            .setMessage("Google will receive an authorization and contact-list request for the account you choose. Only read-only access is requested. Cached matches can allow callers through QuietGate, but do not prove caller identity or trustworthiness. Other contacts are excluded. Continue?")
            .setNegativeButton("Cancel",null).setPositiveButton("Choose account",(d,w)->{
                if(working)return;
                try{startActivityForResult(AccountManager.newChooseAccountIntent(null,null,new String[]{"com.google"},null,null,null,null),CHOOSE);}
                catch(RuntimeException unavailable){notice("Google's account chooser is unavailable on this device.");}
            }).show());
        try{
            List<GoogleContactStore.Entry> entries=GoogleContactStore.entries(this);
            if(entries.isEmpty())text("No Google accounts connected.",16);
            for(GoogleContactStore.Entry e:entries){
                String status=e.fresh()?"Matching active":"Matching paused / expired";
                text(e.account+"\n"+status+" · "+e.hashes.size()+" cached identifiers\nLast successful refresh: "+(e.updated==0?"Never":IncidentExport.utc(e.updated)),16);
                button("Refresh: "+e.account,()->authorize(e.account));
                button("Disconnect and delete: "+e.account,()->disconnect(e.account));
            }
        }catch(Exception unavailable){text("Saved-contact cache cannot be opened. Device contacts remain available. Delete the local cache below before reconnecting.",16);}
        button("Delete all connected-contact data",()->new AlertDialog.Builder(this).setTitle("Delete all local connected contacts?")
            .setMessage("Deletes account labels, snapshots and encryption keys on this phone. Device contacts and incident records stay intact. Google grants must be revoked separately in your Google Account settings if accounts cannot be opened here.")
            .setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{try{GoogleContactStore.clear(this);pendingAccount=null;pendingGeneration=null;working=false;render();notice("Local connected-contact data deleted. Manage any remaining grants at myaccount.google.com/connections.");}catch(Exception e){notice("Could not delete connected-contact data.");}}).show());
        text("Google Cloud setup is required before authorization works: enable People API and register an Android OAuth client for this app and its signing certificate. If setup is missing, connection will fail safely and device contacts will continue working. No Google Cloud setup or approval is claimed by this build.",14);
        button("Google library licenses",()->{
            try(java.io.InputStream in=getAssets().open("GOOGLE-AUTH-THIRD-PARTY-LICENSES.txt");java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){
                byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
                TextView licenses=new TextView(this);licenses.setText(new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8));licenses.setTextIsSelectable(true);
                ScrollView view=new ScrollView(this);view.addView(licenses);new AlertDialog.Builder(this).setTitle("Google library third-party notices").setView(view).setPositiveButton("Close",null).show();
            }catch(java.io.IOException e){notice("License document unavailable. Google SDK terms: developer.android.com/studio/terms.html");}
        });
        if(working)text("Contact refresh in progress…",16);
    }
    private void authorize(String account){
        if(working)return;
        if(REVOKING.contains(account)){notice("Google revocation is still in progress for this account. Please wait before reconnecting.");return;}
        try{
            pendingAccount=account;pendingGeneration=GoogleContactStore.begin(this,account);working=true;render();
            AuthorizationRequest request=AuthorizationRequest.builder().setAccount(new Account(account,"com.google")).setRequestedScopes(scopes()).build();
            final String generation=pendingGeneration;
            Identity.getAuthorizationClient(this).authorize(request).addOnSuccessListener(result->authorized(result,account,generation))
                .addOnFailureListener(e->failedFor(account,generation,"Authorization failed. Google Cloud OAuth/People API setup may be missing, access may have been denied, or Google services may be unavailable."));
        }catch(Exception e){failed("Could not start authorization or open the encrypted contact cache.");}
    }
    private void authorized(AuthorizationResult result,String account,String generation){
        if(isFinishing() || isDestroyed())return;
        if(account==null || generation==null || !account.equals(pendingAccount) || !generation.equals(pendingGeneration))return;
        try{if(!GoogleContactStore.active(this,account,generation))return;}catch(Exception e){failed("Could not open saved connection state.");return;}
        working=true;render();
        if(result.hasResolution()){
            try{startIntentSenderForResult(result.getPendingIntent().getIntentSender(),AUTHORIZE,null,0,0,0);}
            catch(Exception e){failed("Google authorization could not be opened.");}return;
        }
        if(result.getAccessToken()==null || result.getGrantedScopes()==null || !result.getGrantedScopes().contains(GoogleContactsSync.SCOPE)){
            failed("Read-only saved-contact access was not granted. No cloud matches activated.");return;
        }
        final String token=result.getAccessToken();
        final Context app=getApplicationContext();
        new Thread(()->{
            String message;
            try{
                Set<String> hashes=GoogleContactsSync.fetch(app,account,generation,token);
                message=GoogleContactStore.complete(app,account,generation,hashes)?"Saved contacts refreshed. Local matching is active for 24 hours.":"Connection was removed; the downloaded list was discarded.";
            }catch(Exception e){
                if(e instanceof GoogleContactsSync.Unauthorized)Identity.getAuthorizationClient(app).clearToken(ClearTokenRequest.builder().setToken(token).build());
                try{GoogleContactStore.failed(app,account,generation);}catch(Exception ignored){}
                message="Refresh failed. This Google account is not used for matching. Check access, connection and Google Cloud setup, then retry. Device contacts remain available.";
            }
            final String outcome=message;runOnUiThread(()->{
                // A late result must never clear or replace another account's newer authorization.
                if(!account.equals(pendingAccount) || !generation.equals(pendingGeneration))return;
                working=false;pendingAccount=null;pendingGeneration=null;if(!isFinishing() && !isDestroyed()){render();notice(outcome);}
            });
        },"QuietGate-Google-saved-contacts").start();
    }
    private void failed(String message){
        if(pendingAccount!=null && pendingGeneration!=null)try{GoogleContactStore.failed(this,pendingAccount,pendingGeneration);}catch(Exception ignored){}
        working=false;pendingAccount=null;pendingGeneration=null;if(!isFinishing() && !isDestroyed()){render();notice(message);}
    }
    private void failedFor(String account,String generation,String message){
        if(account.equals(pendingAccount) && generation.equals(pendingGeneration))failed(message);
        else try{GoogleContactStore.failed(this,account,generation);}catch(Exception ignored){}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==CHOOSE){
            if(result!=RESULT_OK || data==null)return;
            String name=data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME),type=data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
            if(name==null || !"com.google".equals(type)){notice("Choose a Google account.");return;}authorize(name);
        }else if(request==AUTHORIZE){
            if(result!=RESULT_OK || data==null){failed("Authorization canceled. This account is not used for cloud matching.");return;}
            try{authorized(Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data),pendingAccount,pendingGeneration);}
            catch(ApiException e){failed("Google authorization did not complete. No cloud matches activated.");}
        }
    }
    private void disconnect(String account){
        new AlertDialog.Builder(this).setTitle("Disconnect this account?")
            .setMessage("Removes its local matching cache immediately and asks Google to revoke QuietGate's access for this account. Your Google contacts are not deleted. Google revocation affects all scopes previously granted to this app for the account.")
            .setNegativeButton("Cancel",null).setPositiveButton("Disconnect",(d,w)->{
                if(!REVOKING.add(account))return;
                try{GoogleContactStore.remove(this,account);if(account.equals(pendingAccount)){pendingAccount=null;pendingGeneration=null;working=false;}render();}
                catch(Exception e){REVOKING.remove(account);notice("Local cache deletion failed. Account was not disconnected.");return;}
                RevokeAccessRequest request=RevokeAccessRequest.builder().setAccount(new Account(account,"com.google")).setScopes(scopes()).build();
                Identity.getAuthorizationClient(this).revokeAccess(request)
                    .addOnSuccessListener(v->{REVOKING.remove(account);if(!isFinishing() && !isDestroyed())notice("Local matching removed and Google access revoked.");})
                    .addOnFailureListener(e->{REVOKING.remove(account);if(!isFinishing() && !isDestroyed())notice("Local matching removed. Google revocation could not be confirmed; remove QuietGate at myaccount.google.com/connections.");});
            }).show();
    }
    private void text(String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setPadding(0,12,0,12);panel.addView(v);}
    private void button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);panel.addView(b);b.setOnClickListener(v->action.run());}
    private void notice(String message){new AlertDialog.Builder(this).setMessage(message).setPositiveButton("OK",null).show();}
}
