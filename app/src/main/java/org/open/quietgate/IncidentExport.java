package org.open.quietgate;

import android.content.ContentResolver;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Checksums describe exported bytes, never the truth or origin of their contents. */
final class IncidentExport {
    static final int MAX_ATTACHMENTS=10;
    static final long MAX_FILE_BYTES=10L*1024*1024, MAX_TOTAL_BYTES=25L*1024*1024;
    static final String LIMITATIONS="Caller identities are not authenticated. Decline actions do not confirm that a call ended. "
        +"Categories and notes are user assessments. Imported records are unverified claims. Attachments are user-supplied files; "
        +"QuietGate does not verify their origin or link them independently to calls. Device timestamps may be inaccurate. "
        +"SHA-256 checksums detect changes relative to this manifest; they do not establish authenticity, independent time, "
        +"legal admissibility or an independently verified chain of custody. No caller IP address or location is obtained.";
    static String utc(long time) {
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date(time));
    }
    static final class Attachment {
        final Uri uri;final String name;
        Attachment(Uri uri,String name){this.uri=uri;this.name=name;}
    }
    static void write(File target,ContentResolver resolver,String appVersion,String title,String notes,
                      JSONArray records,List<Attachment> attachments) throws Exception {
        if(records.length()==0 || records.length()>500)throw new IOException("Select 1-500 records.");
        if(attachments.size()>MAX_ATTACHMENTS)throw new IOException("Too many attachments.");
        long created=System.currentTimeMillis();String incident=UUID.randomUUID().toString();
        JSONObject report=new JSONObject().put("format","QuietGate-incident-report").put("version",1)
            .put("incidentId",incident).put("appVersion",appVersion).put("exportedAtUtc",utc(created))
            .put("exportedAtEpochMs",created).put("title",title).put("userNotes",notes).put("limitations",LIMITATIONS);
        JSONArray normalized=new JSONArray();
        StringBuilder summary=new StringBuilder("QuietGate incident report\n\nTitle: ").append(title)
            .append("\nIncident ID: ").append(incident).append("\nExported (device clock, UTC): ").append(utc(created))
            .append("\nQuietGate: ").append(appVersion).append("\n\nUser notes:\n").append(notes)
            .append("\n\nLimitations:\n").append(LIMITATIONS).append("\n\nSelected attempts:\n");
        for(int i=0;i<records.length();i++) {
            JSONObject e=new JSONObject(records.getJSONObject(i).toString());
            e.put("timeUtc",utc(e.getLong("time")));normalized.put(e);
            summary.append("\n").append(i+1).append(". ").append(e.getString("timeUtc"))
                .append("\nIdentity (unverified): ").append(e.optString("identity"))
                .append("\nSource: ").append(e.optString("source"))
                .append("\nImported: ").append(e.optBoolean("imported"))
                .append("\nAction: ").append(e.optString("action"))
                .append("\nCategory (user assessment): ").append(e.optString("category"))
                .append("\nNotes: ").append(e.optString("notes")).append('\n');
        }
        report.put("records",normalized);
        JSONArray metadata=new JSONArray();StringBuilder checksums=new StringBuilder();long total=0;
        try(ZipOutputStream zip=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(target)))) {
            for(int i=0;i<attachments.size();i++) {
                Attachment a=attachments.get(i);
                // Provider names are metadata only, never archive paths.
                String path=String.format(Locale.US,"attachments/%02d.bin",i+1);
                zip.putNextEntry(new ZipEntry(path));
                MessageDigest digest=MessageDigest.getInstance("SHA-256");long size=0;
                try(InputStream input=resolver.openInputStream(a.uri)) {
                    if(input==null)throw new IOException("Attachment unavailable.");
                    byte[] buffer=new byte[16384];int n;
                    while((n=input.read(buffer))!=-1) {
                        size+=n;total+=n;
                        if(size>MAX_FILE_BYTES || total>MAX_TOTAL_BYTES)throw new IOException("Attachment size limit exceeded.");
                        digest.update(buffer,0,n);zip.write(buffer,0,n);
                    }
                }
                zip.closeEntry();String hash=hex(digest.digest());
                checksums.append(hash).append("  ").append(path).append('\n');
                metadata.put(new JSONObject().put("path",path).put("originalName",a.name).put("bytes",size)
                    .put("sha256",hash).put("source","User-selected attachment; origin unverified"));
                summary.append("\nAttachment: ").append(path).append("\nOriginal filename: ").append(a.name)
                    .append("\nBytes: ").append(size).append("\nSHA-256: ").append(hash).append('\n');
            }
            report.put("attachments",metadata);
            add(zip,"report.json",report.toString(2),checksums);
            add(zip,"report.txt",summary.toString(),checksums);
            add(zip,"README.txt","QuietGate incident package\n\n"+LIMITATIONS
                +"\n\nreport.txt is the readable summary; report.json contains structured records. "
                +"Attachments retain original bytes under numbered .bin names; original names are in the report. "
                +"Only open attachments you trust. They may contain sensitive metadata. The ZIP is not encrypted. "
                +"SHA256SUMS.txt covers every other file, but not itself. Keep an independent copy of the manifest. "
                +"After extraction, verify with sha256sum -c SHA256SUMS.txt, or compare individual hashes using "
                +"PowerShell Get-FileHash -Algorithm SHA256.\n",checksums);
            add(zip,"SHA256SUMS.txt",checksums.toString(),null);
        }
    }
    private static void add(ZipOutputStream zip,String name,String text,StringBuilder checksums)throws Exception {
        byte[] bytes=text.getBytes(StandardCharsets.UTF_8);
        zip.putNextEntry(new ZipEntry(name));zip.write(bytes);zip.closeEntry();
        if(checksums!=null)checksums.append(hex(MessageDigest.getInstance("SHA-256").digest(bytes))).append("  ").append(name).append('\n');
    }
    private static String hex(byte[] bytes) {
        StringBuilder b=new StringBuilder();for(byte value:bytes)b.append(String.format(Locale.US,"%02x",value&255));return b.toString();
    }
}
