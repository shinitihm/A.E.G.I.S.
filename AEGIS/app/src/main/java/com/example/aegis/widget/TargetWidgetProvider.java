package com.example.aegis.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.aegis.R;
import com.example.aegis.SplashActivity;
import com.example.aegis.data.ApiClient;
import com.example.aegis.data.model.HeroDetail;
import com.example.aegis.ui.hud.Hud;

import retrofit2.Response;

/** Widget "Alvo do dia". Um toque abre o app pela splash, então a biometria continua obrigatória. */
public class TargetWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {
        show(context, manager, widgetIds, context.getString(R.string.widget_loading), "", 0);

        PendingResult pending = goAsync();
        new Thread(() -> {
            try {
                Response<HeroDetail> response = ApiClient.get().targetOfTheDay().execute();
                HeroDetail hero = response.body();
                if (response.isSuccessful() && hero != null) {
                    int color = Hud.threatColor(context, hero.threat.levelCode);
                    show(context, manager, widgetIds, hero.name.toUpperCase(),
                            hero.threat.level + " · " + hero.threat.score, color);
                } else {
                    show(context, manager, widgetIds, context.getString(R.string.app_name),
                            context.getString(R.string.server_offline), 0);
                }
            } catch (Exception e) {
                show(context, manager, widgetIds, context.getString(R.string.app_name),
                        context.getString(R.string.server_offline), 0);
            } finally {
                pending.finish();
            }
        }).start();
    }

    private void show(Context context, AppWidgetManager manager, int[] ids, String name, String threat, int color) {
        PendingIntent open = PendingIntent.getActivity(context, 0,
                new Intent(context, SplashActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_target);
            views.setTextViewText(R.id.widgetName, name);
            views.setTextViewText(R.id.widgetThreat, threat);
            if (color != 0) views.setTextColor(R.id.widgetThreat, color);
            views.setOnClickPendingIntent(R.id.widgetRoot, open);
            manager.updateAppWidget(id, views);
        }
    }
}
