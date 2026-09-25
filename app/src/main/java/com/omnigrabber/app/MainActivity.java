package com.omnigrabber.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int NEON = Color.rgb(57,255,20);
    private static final int BLACK = Color.rgb(2,5,3);
    private static final int RED = Color.rgb(255,90,90);
    private static final String API_HOST = "social-media-video-downloader.p.rapidapi.com";
    private static final String API_PATH = "/Get-TikTok-Post-Details";

    private LinearLayout root, resultBox;
    private EditText urlInput;
    private TextView status, info, quality;
    private Button grab, download, preview;
    private VideoView bg;
    private MediaPlayer theme;
    private boolean previewOpen = false;
    private String source = "", mediaUrl = "", title = "TikTok_media", kind = "video";
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(BLACK);
        buildUi();
        startTheme();
        status.setText("● TIKTOK ENGINE READY");
        status.setTextColor(NEON);
        quality.setText("TikTok direct + vt.tiktok.com supported");
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
    private android.graphics.drawable.GradientDrawable box(int c,float r,int sc,int sw){
        android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();
        g.setColor(c); g.setCornerRadius(dp(r)); if(sw>0) g.setStroke(dp(sw),sc); return g;
    }
    private TextView tv(String s,float z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.CENTER_VERTICAL);return t;}

    private void buildUi(){
        android.widget.FrameLayout frame=new android.widget.FrameLayout(this);
        bg=new VideoView(this); frame.addView(bg,new android.widget.FrameLayout.LayoutParams(-1,-1));
        int rid=getResources().getIdentifier("omni_bg","raw",getPackageName());
        if(rid!=0){bg.setVideoURI(Uri.parse("android.resource://"+getPackageName()+"/"+rid));bg.setOnPreparedListener(mp->{mp.setLooping(true);mp.setVolume(0,0);mp.start();});}
        View shade=new View(this); shade.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xD9010502,0xA8051208,0xF7010302})); frame.addView(shade,new android.widget.FrameLayout.LayoutParams(-1,-1));
        ScrollView sv=new ScrollView(this); sv.setFillViewport(true); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(28),dp(18),dp(24)); sv.addView(root); frame.addView(sv,new android.widget.FrameLayout.LayoutParams(-1,-1)); setContentView(frame);

        TextView brand=tv("OMNI-GRABBER",31,Color.WHITE);brand.setGravity(Gravity.CENTER);brand.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));brand.setLetterSpacing(.08f);root.addView(brand,new LinearLayout.LayoutParams(-1,dp(58)));glitchLoop(brand);
        TextView sub=tv("TIKTOK DOWNLOADER",10,NEON);sub.setGravity(Gravity.CENTER);sub.setLetterSpacing(.18f);root.addView(sub,new LinearLayout.LayoutParams(-1,dp(28)));

        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackground(box(0xE608140A,20,0x8039FF14,1));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(18);root.addView(card,cp);
        TextView h=tv("PASTE TIKTOK URL",13,NEON);h.setTypeface(Typeface.DEFAULT_BOLD);card.addView(h,new LinearLayout.LayoutParams(-1,dp(30)));
        urlInput=new EditText(this);urlInput.setSingleLine(true);urlInput.setTextColor(Color.WHITE);urlInput.setTextSize(13);urlInput.setHintTextColor(0x7796B89A);urlInput.setHint("https://vt.tiktok.com/... or full TikTok URL");urlInput.setPadding(dp(14),0,dp(14),0);urlInput.setBackground(box(0xCC020604,13,0x5544FF44,1));card.addView(urlInput,new LinearLayout.LayoutParams(-1,dp(54)));
        grab=new Button(this);grab.setText("⚡  GET TIKTOK MEDIA");grab.setTextColor(BLACK);grab.setTextSize(13);grab.setAllCaps(false);grab.setTypeface(Typeface.DEFAULT_BOLD);grab.setBackground(box(NEON,14,0,0));LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,dp(54));gp.topMargin=dp(12);card.addView(grab,gp);grab.setOnClickListener(v->{click(v);analyze();});

        resultBox=new LinearLayout(this);resultBox.setOrientation(LinearLayout.VERTICAL);resultBox.setPadding(dp(16),dp(14),dp(16),dp(14));resultBox.setBackground(box(0xE6081009,18,0x5539FF14,1));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(16);root.addView(resultBox,rp);
        status=tv("● READY",11,NEON);status.setTypeface(Typeface.DEFAULT_BOLD);resultBox.addView(status,new LinearLayout.LayoutParams(-1,dp(30)));
        quality=tv("TikTok direct + vt.tiktok.com supported",10,0xFF7D997F);resultBox.addView(quality,new LinearLayout.LayoutParams(-1,dp(42)));
        info=tv("",11,Color.WHITE);info.setVisibility(View.GONE);resultBox.addView(info,new LinearLayout.LayoutParams(-1,dp(74)));
        LinearLayout actions=new LinearLayout(this);actions.setVisibility(View.GONE);actions.setPadding(0,dp(6),0,0);
        preview=new Button(this);preview.setText("▶ PREVIEW");preview.setAllCaps(false);preview.setTextColor(NEON);preview.setBackground(box(0xAA0A1D0D,12,0x6639FF14,1));
        download=new Button(this);download.setText("↓ DOWNLOAD");download.setAllCaps(false);download.setTextColor(BLACK);download.setBackground(box(NEON,12,0,0));
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(48),1);ap.setMargins(dp(3),0,dp(3),0);actions.addView(preview,ap);actions.addView(download,ap);resultBox.addView(actions,new LinearLayout.LayoutParams(-1,dp(54)));
        preview.setOnClickListener(v->{click(v);previewCurrent();});download.setOnClickListener(v->{click(v);downloadCurrent();});
        TextView foot=tv("OmniGrabber • TikTok mode",9,0x779BFF9B);foot.setGravity(Gravity.CENTER);foot.setPadding(0,dp(18),0,0);root.addView(foot,new LinearLayout.LayoutParams(-1,dp(50)));
    }

    private void startTheme(){try{theme=MediaPlayer.create(this,R.raw.omni_theme);if(theme!=null){theme.setLooping(true);theme.setVolume(.20f,.20f);theme.start();}}catch(Exception ignored){}}
    private void pauseTheme(){try{if(theme!=null&&theme.isPlaying())theme.pause();}catch(Exception ignored){}}
    private void resumeTheme(){try{if(theme!=null&&!theme.isPlaying()&&!previewOpen)theme.start();}catch(Exception ignored){}}
    @Override protected void onResume(){super.onResume();if(!previewOpen)resumeTheme();}
    @Override protected void onPause(){super.onPause();if(!previewOpen)pauseTheme();}
    @Override protected void onDestroy(){io.shutdownNow();try{if(theme!=null){theme.stop();theme.release();}}catch(Exception ignored){}super.onDestroy();}
    private void click(View v){v.animate().scaleX(.96f).scaleY(.96f).setDuration(60).withEndAction(()->v.animate().scaleX(1).scaleY(1).setDuration(110).start()).start();}
    private void glitchLoop(TextView t){Random r=new Random();Runnable[] a=new Runnable[1];a[0]=()->{String base="OMNI-GRABBER";StringBuilder s=new StringBuilder();for(char c:base.toCharArray())s.append(r.nextInt(100)<15?(r.nextBoolean()?"0":"1"):c);t.setText(s);main.postDelayed(a[0],180+r.nextInt(850));};main.postDelayed(a[0],500);}

    private void analyze(){
        String u=urlInput.getText().toString().trim();
        if(!isTikTok(u)){urlInput.setError("Masukkan URL TikTok yang valid");return;}
        source=u; grab.setEnabled(false); grab.setText("RESOLVING..."); status.setText("◉ RESOLVING TIKTOK URL..."); status.setTextColor(NEON); quality.setText("Short link vt.tiktok.com akan diikuti otomatis.");
        io.execute(()->{try{
            String resolved=resolveUrl(u);
            main.post(()->{status.setText("◉ GETTING MEDIA...");quality.setText("Mengambil media TikTok...");});
            JSONObject data=callApi(resolved);
            mediaUrl=findBestMediaUrl(data);
            if(mediaUrl==null) throw new Exception("API tidak mengembalikan direct media URL");
            title=safeName(findFirstText(data,"title","desc","description","caption"));
            if(title.equals("TikTok_media")) title="TikTok_"+UUID.randomUUID().toString().substring(0,8);
            kind="video";
            main.post(this::showReady);
        }catch(Exception e){main.post(()->fail(explain(e)));}});
    }

    private boolean isTikTok(String u){try{URL x=new URL(u);String h=x.getHost().toLowerCase(Locale.US);return h.equals("tiktok.com")||h.endsWith(".tiktok.com");}catch(Exception e){return false;}}

    private String resolveUrl(String raw)throws Exception{
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL(raw).openConnection();
            c.setInstanceFollowRedirects(true); c.setConnectTimeout(10000); c.setReadTimeout(10000);
            c.setRequestMethod("GET"); c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/130 Mobile Safari/537.36");
            c.connect();
            int code=c.getResponseCode();
            String finalUrl=c.getURL().toString();
            if(code>=400) throw new Exception("TikTok short URL HTTP "+code);
            return finalUrl;
        }finally{if(c!=null)c.disconnect();}
    }

    private JSONObject callApi(String tiktokUrl)throws Exception{
        String key=BuildConfig.RAPIDAPI_KEY;
        if(key==null||key.trim().isEmpty()) throw new Exception("RapidAPI configuration belum tersedia");
        String q=URLEncoder.encode(tiktokUrl,"UTF-8");
        URL u=new URL("https://"+API_HOST+API_PATH+"?url="+q);
        HttpURLConnection c=(HttpURLConnection)u.openConnection();
        c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setRequestMethod("GET");
        c.setRequestProperty("X-RapidAPI-Key",key);c.setRequestProperty("X-RapidAPI-Host",API_HOST);c.setRequestProperty("Accept","application/json");
        int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String body=read(in);c.disconnect();
        if(code<200||code>=300) throw new Exception("RapidAPI HTTP "+code+(body.isEmpty()?"":" • "+body.substring(0,Math.min(180,body.length()))));
        if(body.trim().isEmpty())throw new Exception("RapidAPI mengembalikan response kosong");
        return new JSONObject(body);
    }

    private String findBestMediaUrl(JSONObject root){List<Candidate> list=new ArrayList<>();collect(root,"",list);Candidate best=null;for(Candidate c:list){if(!isHttp(c.url))continue;String x=c.url.toLowerCase(Locale.US);if(x.contains("avatar")||x.contains("profile")||x.contains("cover")||x.contains("music")||x.contains("author"))continue;int score=c.score;if(x.contains("mp4"))score+=30;if(x.contains("video"))score+=20;if(x.contains("play"))score+=25;if(x.contains("download"))score+=25;if(x.contains("wm"))score-=20;if(x.contains("watermark"))score-=25;if(best==null||score>best.score)best=new Candidate(c.url,score);}return best==null?null:best.url;}
    private void collect(Object v,String key,List<Candidate> out){if(v instanceof JSONObject){JSONObject o=(JSONObject)v;JSONArray names=o.names();if(names!=null)for(int i=0;i<names.length();i++){String k=names.optString(i);Object x=o.opt(k);collect(x,k,out);}}else if(v instanceof JSONArray){JSONArray a=(JSONArray)v;for(int i=0;i<a.length();i++)collect(a.opt(i),key,out);}else if(v instanceof String){String s=(String)v;if(isHttp(s)){String k=key==null?"":key.toLowerCase(Locale.US);int score=0;if(k.contains("no_watermark")||k.contains("nowatermark"))score+=100;if(k.contains("download"))score+=70;if(k.contains("play"))score+=60;if(k.equals("url"))score+=20;if(k.contains("video"))score+=40;if(k.contains("image")||k.contains("avatar")||k.contains("cover"))score-=50;out.add(new Candidate(s,score));}}}
    private static class Candidate{String url;int score;Candidate(String u,int s){url=u;score=s;}}
    private boolean isHttp(String s){return s!=null&&(s.startsWith("http://")||s.startsWith("https://"));}

    private String findFirstText(JSONObject o,String...keys){for(String key:keys){String v=findTextRecursive(o,key.toLowerCase(Locale.US));if(v!=null&&!v.trim().isEmpty())return v;}return "TikTok_media";}
    private String findTextRecursive(Object v,String wanted){if(v instanceof JSONObject){JSONObject o=(JSONObject)v;JSONArray names=o.names();if(names!=null)for(int i=0;i<names.length();i++){String k=names.optString(i);Object x=o.opt(k);if(k.toLowerCase(Locale.US).equals(wanted)&&x instanceof String)return (String)x;String r=findTextRecursive(x,wanted);if(r!=null)return r;}}else if(v instanceof JSONArray){JSONArray a=(JSONArray)v;for(int i=0;i<a.length();i++){String r=findTextRecursive(a.opt(i),wanted);if(r!=null)return r;}}return null;}

    private void showReady(){
        info.setVisibility(View.VISIBLE);info.setText("TIKTOK MEDIA READY\n"+title+"\nDirect media URL found");
        ((LinearLayout)resultBox.getChildAt(resultBox.getChildCount()-1)).setVisibility(View.VISIBLE);
        status.setText("✓ SOURCE READY");status.setTextColor(NEON);quality.setText("Preview/download siap.");grab.setEnabled(true);grab.setText("⚡  GET TIKTOK MEDIA");
    }

    private void downloadCurrent(){if(mediaUrl.isEmpty()){fail("Media belum tersedia");return;}download.setEnabled(false);status.setText("◉ DOWNLOADING...");quality.setText("Menyimpan media TikTok ke Download/OmniGrabber...");io.execute(()->{try{File tmp=downloadTemp(mediaUrl);String saved=saveToDownloads(tmp,title);try{tmp.delete();}catch(Exception ignored){}main.post(()->{status.setText("✓ DOWNLOAD COMPLETE");status.setTextColor(NEON);quality.setText("Saved: "+saved);download.setEnabled(true);});}catch(Exception e){main.post(()->{download.setEnabled(true);fail(explain(e));});}});}

    private File downloadTemp(String u)throws Exception{File dir=new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"OmniGrabber");if(!dir.exists()&&!dir.mkdirs())throw new Exception("Folder sementara gagal dibuat");String ext="mp4";String low=u.toLowerCase(Locale.US);if(low.contains(".jpg")||low.contains(".jpeg")||low.contains(".png")||low.contains(".webp"))ext="jpg";File f=new File(dir,"tiktok_"+System.currentTimeMillis()+"."+ext);HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","Mozilla/5.0");int code=c.getResponseCode();if(code<200||code>=300)throw new Exception("Media HTTP "+code);try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(f)){copy(in,out);}c.disconnect();if(f.length()==0)throw new Exception("File media kosong");return f;}
    private String saveToDownloads(File src,String base)throws Exception{String ext=extension(src.getName());String filename=safeName(base)+"."+ext;if(Build.VERSION.SDK_INT>=29){android.content.ContentValues v=new android.content.ContentValues();v.put(MediaStore.MediaColumns.DISPLAY_NAME,filename);v.put(MediaStore.MediaColumns.MIME_TYPE,ext.equals("jpg")?"image/jpeg":"video/mp4");v.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/OmniGrabber");Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(uri==null)throw new Exception("MediaStore gagal");try(InputStream in=new FileInputStream(src);OutputStream out=getContentResolver().openOutputStream(uri)){copy(in,out);}return "Download/OmniGrabber/"+filename;}else{File dir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"OmniGrabber");if(!dir.exists()&&!dir.mkdirs())throw new Exception("Folder Downloads gagal");File dst=new File(dir,filename);try(InputStream in=new FileInputStream(src);OutputStream out=new FileOutputStream(dst)){copy(in,out);}return dst.getAbsolutePath();}}
    private String read(InputStream in)throws Exception{if(in==null)return "";StringBuilder s=new StringBuilder();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)s.append(new String(b,0,n,StandardCharsets.UTF_8));return s.toString();}
    private void copy(InputStream in,OutputStream out)throws Exception{byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);out.flush();}
    private String extension(String n){int p=n.lastIndexOf('.');return p>0?n.substring(p+1).toLowerCase(Locale.US):"mp4";}

    private void previewCurrent(){if(mediaUrl.isEmpty())return;previewOpen=true;pauseTheme();Dialog d=new Dialog(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(12),dp(12),dp(12));box.setBackground(box(0xF5070E08,18,0x9939FF14,1));TextView h=tv("TIKTOK PREVIEW",14,NEON);h.setTypeface(Typeface.DEFAULT_BOLD);box.addView(h,new LinearLayout.LayoutParams(-1,dp(38)));box.addView(tv(title,10,0xFFB9D4BC),new LinearLayout.LayoutParams(-1,dp(40)));VideoView vv=new VideoView(this);vv.setVideoURI(Uri.parse(mediaUrl));vv.setMediaController(new android.widget.MediaController(this));vv.setOnErrorListener((mp,w,e)->{status.setText("✕ Preview gagal. Download tetap bisa.");status.setTextColor(RED);return true;});vv.setOnPreparedListener(mp->{mp.setVolume(1f,1f);vv.start();});box.addView(vv,new LinearLayout.LayoutParams(-1,dp(280)));Button close=new Button(this);close.setText("CLOSE PREVIEW");close.setAllCaps(false);close.setTextColor(NEON);close.setBackground(this.box(0xAA0A1D0D,12,0x6639FF14,1));box.addView(close,new LinearLayout.LayoutParams(-1,dp(48)));close.setOnClickListener(v->{click(v);d.dismiss();});d.setContentView(box);if(d.getWindow()!=null)d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);d.setOnDismissListener(x->{previewOpen=false;resumeTheme();});d.show();if(d.getWindow()!=null)d.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*.94),WindowManager.LayoutParams.WRAP_CONTENT);}

    private void fail(String s){status.setText("✕ "+s);status.setTextColor(RED);grab.setEnabled(true);grab.setText("⚡  GET TIKTOK MEDIA");}
    private String explain(Exception e){String s=e==null?"Unknown error":(e.getMessage()==null?e.toString():e.getMessage());s=s.replace("java.io.IOException:","").replace("java.net.","").replace("\n"," ").trim();return s.length()>260?s.substring(0,260):s;}
    private String safeName(String s){if(s==null||s.trim().isEmpty())s="TikTok_media";s=s.replaceAll("[\\\\/:*?\"<>|]","_").replaceAll("\\s+"," ").trim();if(s.length()>90)s=s.substring(0,90);return s;}
}
