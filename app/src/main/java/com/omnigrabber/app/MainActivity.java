package com.omnigrabber.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.VideoView;

import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private static final int NEON=Color.rgb(57,255,20), BLACK=Color.rgb(2,5,3), RED=Color.rgb(255,90,90);
    private LinearLayout root,resultBox;
    private EditText urlInput;
    private TextView status,info,quality;
    private Button grab,download,preview;
    private VideoView bg;
    private MediaPlayer theme;
    private boolean previewOpen=false,engineReady=false;
    private String source="",title="OmniGrabber_media",kind="video";
    private String mode="AUTO";
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());

    @Override protected void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(BLACK);buildUi();initEngine();startTheme();}
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private android.graphics.drawable.GradientDrawable box(int c,float r,int sc,int sw){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));if(sw>0)g.setStroke(dp(sw),sc);return g;}
    private TextView tv(String s,float z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setGravity(Gravity.CENTER_VERTICAL);return t;}

    private void buildUi(){
        android.widget.FrameLayout frame=new android.widget.FrameLayout(this);
        bg=new VideoView(this);frame.addView(bg,new android.widget.FrameLayout.LayoutParams(-1,-1));
        int rid=getResources().getIdentifier("omni_bg","raw",getPackageName());
        if(rid!=0){bg.setVideoURI(Uri.parse("android.resource://"+getPackageName()+"/"+rid));bg.setOnPreparedListener(mp->{mp.setLooping(true);mp.setVolume(0,0);mp.start();});}
        View shade=new View(this);shade.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xD9010502,0xA8051208,0xF7010302}));frame.addView(shade,new android.widget.FrameLayout.LayoutParams(-1,-1));
        ScrollView sv=new ScrollView(this);sv.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(28),dp(18),dp(24));sv.addView(root);frame.addView(sv,new android.widget.FrameLayout.LayoutParams(-1,-1));setContentView(frame);
        TextView brand=tv("OMNI-GRABBER",31,Color.WHITE);brand.setGravity(Gravity.CENTER);brand.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));brand.setLetterSpacing(.08f);root.addView(brand,new LinearLayout.LayoutParams(-1,dp(58)));glitchLoop(brand);
        TextView sub=tv("NATIVE YT-DLP MEDIA ENGINE",10,NEON);sub.setGravity(Gravity.CENTER);sub.setLetterSpacing(.18f);root.addView(sub,new LinearLayout.LayoutParams(-1,dp(28)));

        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackground(box(0xE608140A,20,0x8039FF14,1));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(18);root.addView(card,cp);
        TextView h=tv("PASTE SOURCE URL",13,NEON);h.setTypeface(Typeface.DEFAULT_BOLD);card.addView(h,new LinearLayout.LayoutParams(-1,dp(30)));
        urlInput=new EditText(this);urlInput.setSingleLine(true);urlInput.setTextColor(Color.WHITE);urlInput.setTextSize(13);urlInput.setHintTextColor(0x7796B89A);urlInput.setHint("TikTok / YouTube / Instagram / X / other supported sites");urlInput.setPadding(dp(14),0,dp(14),0);urlInput.setBackground(box(0xCC020604,13,0x5544FF44,1));card.addView(urlInput,new LinearLayout.LayoutParams(-1,dp(54)));
        grab=new Button(this);grab.setText("⚡  ANALYZE & GRAB");grab.setTextColor(BLACK);grab.setTextSize(13);grab.setAllCaps(false);grab.setTypeface(Typeface.DEFAULT_BOLD);grab.setBackground(box(NEON,14,0,0));LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,dp(54));gp.topMargin=dp(12);card.addView(grab,gp);grab.setOnClickListener(v->{click(v);analyze();});
        LinearLayout chips=new LinearLayout(this);chips.setGravity(Gravity.CENTER);chips.setPadding(0,dp(12),0,0);for(String s:new String[]{"AUTO","VIDEO","AUDIO","PHOTO","HD / 4K"}){TextView c=tv(s,9,0xFFD9FFD9);c.setGravity(Gravity.CENTER);c.setBackground(box(0x99132616,30,0x5539FF14,1));c.setOnClickListener(v->{click(v);mode=((TextView)v).getText().toString();setChipState(chips);});LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(0,dp(34),1);q.setMargins(dp(2),0,dp(2),0);chips.addView(c,q);}card.addView(chips);

        resultBox=new LinearLayout(this);resultBox.setOrientation(LinearLayout.VERTICAL);resultBox.setPadding(dp(16),dp(14),dp(16),dp(14));resultBox.setBackground(box(0xE6081009,18,0x5539FF14,1));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(16);root.addView(resultBox,rp);
        status=tv("● ENGINE STARTING...",11,0xFF9BFF9B);status.setTypeface(Typeface.DEFAULT_BOLD);resultBox.addView(status,new LinearLayout.LayoutParams(-1,dp(30)));
        quality=tv("Real yt-dlp extraction • no Cobalt • no public-instance race",10,0xFF7D997F);resultBox.addView(quality,new LinearLayout.LayoutParams(-1,dp(42)));
        info=tv("",11,Color.WHITE);info.setVisibility(View.GONE);resultBox.addView(info,new LinearLayout.LayoutParams(-1,dp(62)));
        LinearLayout actions=new LinearLayout(this);actions.setVisibility(View.GONE);actions.setPadding(0,dp(6),0,0);
        preview=new Button(this);preview.setText("▶ PREVIEW");preview.setAllCaps(false);preview.setTextColor(NEON);preview.setBackground(box(0xAA0A1D0D,12,0x6639FF14,1));
        download=new Button(this);download.setText("↓ DOWNLOAD");download.setAllCaps(false);download.setTextColor(BLACK);download.setBackground(box(NEON,12,0,0));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(48),1);ap.setMargins(dp(3),0,dp(3),0);actions.addView(preview,ap);actions.addView(download,ap);resultBox.addView(actions,new LinearLayout.LayoutParams(-1,dp(54)));
        preview.setOnClickListener(v->{click(v);previewCurrent();});download.setOnClickListener(v->{click(v);downloadCurrent();});
        TextView foot=tv("OmniGrabber 1.4 • Native yt-dlp • Save only media you have the right to download.",9,0x779BFF9B);foot.setGravity(Gravity.CENTER);foot.setPadding(0,dp(18),0,0);root.addView(foot,new LinearLayout.LayoutParams(-1,dp(50)));
    }
    private void setChipState(LinearLayout row){for(int i=0;i<row.getChildCount();i++){TextView c=(TextView)row.getChildAt(i);boolean on=c.getText().toString().equals(mode);c.setTextColor(on?BLACK:0xFFD9FFD9);c.setBackground(box(on?NEON:0x99132616,30,0x5539FF14,1));}}
    private void initEngine(){io.execute(()->{try{YoutubeDL.getInstance().init(getApplicationContext());engineReady=true;main.post(()->{status.setText("● READY / NATIVE ENGINE ONLINE");quality.setText("yt-dlp engine active • Cobalt completely removed • crash-safe worker");});}catch(Exception e){main.post(()->fail("Engine init gagal: "+safeErr(e)));}});}
    private void startTheme(){try{theme=MediaPlayer.create(this,R.raw.omni_theme);if(theme!=null){theme.setLooping(true);theme.setVolume(.20f,.20f);theme.start();}}catch(Exception ignored){}}
    private void pauseTheme(){try{if(theme!=null&&theme.isPlaying())theme.pause();}catch(Exception ignored){}}
    private void resumeTheme(){try{if(theme!=null&&!theme.isPlaying()&&!previewOpen)theme.start();}catch(Exception ignored){}}
    @Override protected void onResume(){super.onResume();if(!previewOpen)resumeTheme();}
    @Override protected void onPause(){super.onPause();if(!previewOpen)pauseTheme();}
    @Override protected void onDestroy(){io.shutdownNow();try{if(theme!=null){theme.stop();theme.release();}}catch(Exception ignored){}super.onDestroy();}
    private void click(View v){v.animate().scaleX(.96f).scaleY(.96f).setDuration(60).withEndAction(()->v.animate().scaleX(1).scaleY(1).setDuration(110).start()).start();}
    private void glitchLoop(TextView t){Random r=new Random();Runnable[] a=new Runnable[1];a[0]=()->{String base="OMNI-GRABBER";StringBuilder s=new StringBuilder();for(char c:base.toCharArray())s.append(r.nextInt(100)<15?(r.nextBoolean()?"0":"1"):c);t.setText(s);main.postDelayed(a[0],180+r.nextInt(850));};main.postDelayed(a[0],500);}

    private void analyze(){
        String u=urlInput.getText().toString().trim();if(!u.matches("(?i)^https?://.+")){urlInput.setError("Masukkan URL http/https");return;}source=u;grab.setEnabled(false);grab.setText("EXTRACTING...");status.setText("◉ EXTRACTING WITH NATIVE YT-DLP...");status.setTextColor(NEON);quality.setText("Tidak memakai Cobalt. Tidak memakai daftar instance mati. Mohon tunggu...");
        io.execute(()->{try{if(!engineReady)YoutubeDL.getInstance().init(getApplicationContext());JSONObject meta=extractInfo(u);title=safeName(meta.optString("title","OmniGrabber_media"));kind=detectKind(meta);main.post(()->showReady(meta));}catch(Exception e){main.post(()->fail(explain(e)));}});
    }
    private JSONObject extractInfo(String u)throws Exception{
        YoutubeDLRequest r=new YoutubeDLRequest(u);r.addOption("--no-playlist");r.addOption("--no-warnings");r.addOption("--ignore-errors");r.addOption("--dump-single-json");r.addOption("--skip-download");r.addOption("--no-check-certificates");
        YoutubeDLResponse res=YoutubeDL.getInstance().execute(r);String out=res.getOut();if(out==null||out.trim().isEmpty())throw new Exception("yt-dlp tidak mengembalikan metadata");
        String json=lastJson(out);return new JSONObject(json);
    }
    private String lastJson(String s){
        String[] lines=s.split("\\R");
        for(int i=lines.length-1;i>=0;i--){String line=lines[i].trim();if(line.startsWith("{")&&line.endsWith("}"))return line;}
        int a=s.indexOf('{'),b=s.lastIndexOf('}');if(a>=0&&b>a)return s.substring(a,b+1);return s.trim();
    }
    private String detectKind(JSONObject o){String e=o.optString("ext","").toLowerCase(Locale.US),t=o.optString("_type","");if(t.equals("playlist"))return "video";if(e.matches("jpg|jpeg|png|webp|gif"))return "photo";if(e.matches("mp3|m4a|aac|wav|opus|ogg|flac"))return "audio";return "video";}
    private void showReady(JSONObject meta){resultBox.getChildAt(resultBox.getChildCount()-1);info.setVisibility(View.VISIBLE);info.setText("MEDIA READY\n"+title+"\nDetected: "+kind.toUpperCase(Locale.US)+" • mode: "+mode);((LinearLayout)resultBox.getChildAt(resultBox.getChildCount()-2)).setVisibility(View.VISIBLE);status.setText("✓ SOURCE READY");status.setTextColor(NEON);quality.setText("Engine: yt-dlp • best available source will be selected at download time");grab.setEnabled(true);grab.setText("⚡  ANALYZE & GRAB");}

    private void downloadCurrent(){if(source.isEmpty()){fail("Belum ada source");return;}download.setEnabled(false);status.setText("◉ DOWNLOADING • NATIVE ENGINE...");quality.setText("Memilih kualitas tertinggi yang tersedia, sampai 4K jika source menyediakan.");io.execute(()->{File tmp=null;try{tmp=runDownload(source,mode,title);if(tmp==null||!tmp.exists()||tmp.length()==0)throw new Exception("File hasil download kosong");String saved=saveToDownloads(tmp,title,kind);File x=tmp;main.post(()->{status.setText("✓ DOWNLOAD COMPLETE");status.setTextColor(NEON);quality.setText("Saved: "+saved);download.setEnabled(true);try{x.delete();}catch(Exception ignored){}});}catch(Exception e){main.post(()->{download.setEnabled(true);fail("Download gagal: "+explain(e));});}});}
    private File runDownload(String u,String m,String base)throws Exception{
        File dir=new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"OmniGrabber");if(!dir.exists()&&!dir.mkdirs())throw new Exception("Tidak bisa membuat folder sementara");String out=new File(dir,safeName(base)+".%(ext)s").getAbsolutePath();YoutubeDLRequest r=new YoutubeDLRequest(u);r.addOption("--no-playlist");r.addOption("--no-mtime");r.addOption("--no-part");r.addOption("--windows-filenames");r.addOption("--restrict-filenames");r.addOption("-o",out);
        if("AUDIO".equals(m)){r.addOption("-x");r.addOption("--audio-format","mp3");r.addOption("-f","bestaudio/best");}
        else if("PHOTO".equals(m)){r.addOption("-f","best");}
        else {r.addOption("-f","bestvideo*[height<=2160]+bestaudio/best[height<=2160]/best");r.addOption("--merge-output-format","mp4");}
        r.addOption("--no-warnings");r.addOption("--no-check-certificates");YoutubeDLResponse res=YoutubeDL.getInstance().execute(r);String outText=res.getOut();if(outText!=null&&outText.toLowerCase(Locale.US).contains("error")){
            // Do not trust a log line alone if yt-dlp still produced a valid file.
        }
        File[] fs=dir.listFiles((d,n)->n.toLowerCase(Locale.US).startsWith(safeName(base).toLowerCase(Locale.US)));if(fs==null||fs.length==0)fs=dir.listFiles();if(fs==null||fs.length==0)throw new Exception("yt-dlp selesai tetapi file tidak ditemukan");File best=fs[0];for(File f:fs)if(f.isFile()&&f.length()>best.length())best=f;return best;
    }

    private String saveToDownloads(File src,String base,String k)throws Exception{
        String ext=extension(src.getName());String filename=safeName(base)+"."+ext;if(Build.VERSION.SDK_INT>=29){android.content.ContentValues v=new android.content.ContentValues();v.put(MediaStore.MediaColumns.DISPLAY_NAME,filename);v.put(MediaStore.MediaColumns.MIME_TYPE,mime(k,ext));v.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/OmniGrabber");Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(uri==null)throw new Exception("MediaStore gagal membuat file");try(InputStream in=new FileInputStream(src);OutputStream out=getContentResolver().openOutputStream(uri)){copy(in,out);}return "Download/OmniGrabber/"+filename;}else{File dir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"OmniGrabber");if(!dir.exists()&&!dir.mkdirs())throw new Exception("Folder Downloads gagal dibuat");File dst=new File(dir,filename);try(InputStream in=new FileInputStream(src);OutputStream out=new FileOutputStream(dst)){copy(in,out);}return dst.getAbsolutePath();}}
    private void copy(InputStream in,OutputStream out)throws Exception{byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);out.flush();}
    private String extension(String n){int p=n.lastIndexOf('.');return p>0?n.substring(p+1).toLowerCase(Locale.US):("audio".equals(kind)?"mp3":"mp4");}
    private String mime(String k,String e){if("photo".equals(k))return "image/*";if("audio".equals(k))return "audio/mpeg";return "video/*";}

    private void previewCurrent(){if(source.isEmpty())return;previewOpen=true;if(!"photo".equals(kind))pauseTheme();Dialog d=new Dialog(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(12),dp(12),dp(12));box.setBackground(box(0xF5070E08,18,0x9939FF14,1));TextView h=tv("OMNI PREVIEW",14,NEON);h.setTypeface(Typeface.DEFAULT_BOLD);box.addView(h,new LinearLayout.LayoutParams(-1,dp(38)));box.addView(tv(title,10,0xFFB9D4BC),new LinearLayout.LayoutParams(-1,dp(40)));
        if("photo".equals(kind)){ImageView iv=new ImageView(this);iv.setImageResource(R.drawable.omni_logo);iv.setScaleType(ImageView.ScaleType.FIT_CENTER);iv.setAdjustViewBounds(true);box.addView(iv,new LinearLayout.LayoutParams(-1,dp(260)));loadImage(iv,source);}
        else {VideoView vv=new VideoView(this);vv.setVideoURI(Uri.parse(source));vv.setMediaController(new android.widget.MediaController(this));vv.setOnErrorListener((mp,w,e)->{status.setText("✕ Preview gagal. Download tetap bisa dicoba.");status.setTextColor(RED);return true;});vv.setOnPreparedListener(mp->{mp.setVolume("audio".equals(kind)?1f:1f,1f);vv.start();});box.addView(vv,new LinearLayout.LayoutParams(-1,dp(260)));}
        Button close=new Button(this);close.setText("CLOSE PREVIEW");close.setAllCaps(false);close.setTextColor(NEON);close.setBackground(this.box(0xAA0A1D0D,12,0x6639FF14,1));box.addView(close,new LinearLayout.LayoutParams(-1,dp(48)));close.setOnClickListener(v->{click(v);d.dismiss();});d.setContentView(box);Window w=d.getWindow();if(w!=null)w.setBackgroundDrawableResource(android.R.color.transparent);d.setOnDismissListener(x->{previewOpen=false;if(!"photo".equals(kind))resumeTheme();});d.show();if(d.getWindow()!=null)d.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*.94),WindowManager.LayoutParams.WRAP_CONTENT);}

    private void loadImage(ImageView iv,String u){io.execute(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(6000);c.setReadTimeout(9000);c.setRequestProperty("User-Agent","Mozilla/5.0");InputStream in=c.getInputStream();Bitmap b=BitmapFactory.decodeStream(in);in.close();c.disconnect();main.post(()->{if(b!=null)iv.setImageBitmap(b);});}catch(Exception ignored){}});}

    private void fail(String s){status.setText("✕ "+s);status.setTextColor(RED);grab.setEnabled(true);grab.setText("⚡  ANALYZE & GRAB");}
    private String explain(Exception e){String s=e==null?"Unknown error":safeErr(e);if(s.contains("Sign in to confirm")||s.contains("bot"))return "Platform meminta verifikasi/bot check. Ini batas source publik, bukan crash aplikasi.";if(s.toLowerCase(Locale.US).contains("unsupported url"))return "Platform/URL belum didukung yt-dlp versi engine ini.";return s;}
    private String safeErr(Exception e){String s=e==null?"Unknown error":(e.getMessage()==null?e.toString():e.getMessage());s=s.replace("com.yausername.youtubedl_android.YoutubeDLException:","").replace("java.io.IOException:","").replace("\n"," ").trim();return s.length()>240?s.substring(0,240):s;}
    private String safeName(String s){if(s==null||s.trim().isEmpty())s="OmniGrabber_media";s=s.replaceAll("[\\\\/:*?\"<>|]","_").replaceAll("\\s+"," ").trim();if(s.length()>90)s=s.substring(0,90);return s;}
}
