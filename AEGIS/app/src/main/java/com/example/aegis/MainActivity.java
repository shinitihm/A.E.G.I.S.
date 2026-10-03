package com.example.aegis;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.aegis.data.ApiCallback;
import com.example.aegis.data.ApiClient;
import com.example.aegis.data.model.Health;
import com.example.aegis.ui.arsenal.ArsenalFragment;
import com.example.aegis.ui.briefing.BriefingDialogFragment;
import com.example.aegis.ui.heroes.HeroDetailActivity;
import com.example.aegis.ui.heroes.HeroesFragment;
import com.example.aegis.ui.hud.Hud;
import com.example.aegis.ui.jarvis.JarvisFragment;
import com.example.aegis.ui.missions.MissionsFragment;
import com.example.aegis.ui.search.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/** HUD principal: 5 abas mantidas em memória (show/hide) para não perder rolagem nem conversa. */
public class MainActivity extends AppCompatActivity {

    private static final String[] TAGS = {"targets", "search", "jarvis", "arsenal", "missions"};

    private BottomNavigationView nav;
    private View flash;
    private TextView statusOnline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Hud.setContent(this, R.layout.activity_main);
        nav = findViewById(R.id.bottomNav);
        flash = findViewById(R.id.flash);
        statusOnline = findViewById(R.id.statusOnline);

        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            FragmentTransaction tx = fm.beginTransaction();
            add(tx, new HeroesFragment(), 0);
            add(tx, new SearchFragment(), 1);
            add(tx, new JarvisFragment(), 2);
            add(tx, new ArsenalFragment(), 3);
            add(tx, new MissionsFragment(), 4);
            tx.commit();
            fm.executePendingTransactions();
            new BriefingDialogFragment().show(fm, BriefingDialogFragment.TAG); // só na abertura a frio
        }
        nav.setOnItemSelectedListener(item -> {
            select(item.getItemId());
            return true;
        });
        select(nav.getSelectedItemId());
        checkServer();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /** "Perguntar à J.A.R.V.I.S." na ficha de um herói: abre a aba do chat já com a pergunta. */
    private void handleIntent(android.content.Intent intent) {
        String about = intent.getStringExtra(HeroDetailActivity.RESULT_ASK);
        if (about == null) return;
        intent.removeExtra(HeroDetailActivity.RESULT_ASK);
        showTab(R.id.nav_jarvis);
        nav.post(() -> fragment("jarvis", JarvisFragment.class).ask("Quem é " + about + "?"));
    }

    private void add(FragmentTransaction tx, Fragment fragment, int index) {
        tx.add(R.id.container, fragment, TAGS[index]).hide(fragment);
    }

    private int indexOf(@IdRes int itemId) {
        if (itemId == R.id.nav_search) return 1;
        if (itemId == R.id.nav_jarvis) return 2;
        if (itemId == R.id.nav_arsenal) return 3;
        if (itemId == R.id.nav_missions) return 4;
        return 0;
    }

    private void select(@IdRes int itemId) {
        int selected = indexOf(itemId);
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        for (int i = 0; i < TAGS.length; i++) {
            Fragment f = fm.findFragmentByTag(TAGS[i]);
            if (f == null) continue;
            if (i == selected) tx.show(f);
            else tx.hide(f);
        }
        tx.commit();
    }

    /** Usado pela J.A.R.V.I.S. e pelos atalhos para trocar de aba. */
    public void showTab(@IdRes int itemId) {
        nav.setSelectedItemId(itemId);
    }

    public View flashOverlay() {
        return flash;
    }

    public <T extends Fragment> T fragment(String tag, Class<T> type) {
        return type.cast(getSupportFragmentManager().findFragmentByTag(tag));
    }

    public ArsenalFragment arsenal() {
        return fragment("arsenal", ArsenalFragment.class);
    }

    private void checkServer() {
        ApiClient.get().health().enqueue(new ApiCallback<Health>() {
            @Override
            public void onSuccess(Health body) {
                statusOnline.setText(getString(R.string.status_online));
                statusOnline.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.aegis_cyan));
            }

            @Override
            public void onError(String message) {
                statusOnline.setText("● OFFLINE");
                statusOnline.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.aegis_red));
            }
        });
    }
}
