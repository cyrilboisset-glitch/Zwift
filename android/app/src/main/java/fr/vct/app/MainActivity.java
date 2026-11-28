package fr.vct.app;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.trusted.ScreenOrientation;
import androidx.browser.trusted.TrustedWebActivityDisplayMode;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;

import com.google.androidbrowserhelper.trusted.TwaLauncher;
import com.google.androidbrowserhelper.trusted.splashscreens.PwaWrapperSplashScreenStrategy;

/** Ouvre l'appli dans Chrome (pas dans le navigateur par défaut : Brave, etc. bloquent le Bluetooth web). */
public class MainActivity extends Activity {
    static final String URL = "https://cyrilboisset-glitch.github.io/App-ht/";
    static final String[] CHROMES = {"com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary"};
    private TwaLauncher launcher;
    private boolean launched;

    private String chrome() {
        PackageManager pm = getPackageManager();
        for (String p : CHROMES) {
            try { if (pm.getApplicationInfo(p, 0).enabled) return p; } catch (Exception e) { }
        }
        return null; // pas de Chrome : navigateur par défaut
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (b != null && b.getBoolean("launched")) { launched = true; return; }
        int bg = Color.parseColor("#0F0E0D");
        TrustedWebActivityIntentBuilder tb = new TrustedWebActivityIntentBuilder(Uri.parse(URL))
            .setDefaultColorSchemeParams(new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(bg).setNavigationBarColor(bg).build())
            .setDisplayMode(new TrustedWebActivityDisplayMode.ImmersiveMode(true,
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES))
            .setScreenOrientation(ScreenOrientation.LANDSCAPE);
        launcher = new TwaLauncher(this, chrome());
        PwaWrapperSplashScreenStrategy splash = new PwaWrapperSplashScreenStrategy(this, R.drawable.splash, bg,
            ImageView.ScaleType.CENTER, null, 300, "fr.vct.app.fileprovider");
        launcher.launch(tb, null, splash, () -> launched = true, TwaLauncher.CCT_FALLBACK_STRATEGY);
    }

    @Override protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); o.putBoolean("launched", launched); }

    // retour depuis Chrome (bouton retour) : on ferme l'écran de lancement
    @Override protected void onRestart() { super.onRestart(); if (launched) finish(); }

    @Override protected void onDestroy() { super.onDestroy(); if (launcher != null) launcher.destroy(); }
}
