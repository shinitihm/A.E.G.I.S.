package com.example.aegis.ui.jarvis;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aegis.R;
import com.example.aegis.data.model.ChatTurn;
import com.example.aegis.ui.hud.Hud;

import java.util.List;

/** Bolhas do chat. A última resposta da J.A.R.V.I.S. aparece letra por letra. */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.Holder> {

    private final List<ChatTurn> turns;
    private int animatePosition = -1;

    public ChatAdapter(List<ChatTurn> turns) {
        this.turns = turns;
    }

    /** Marca a mensagem que deve ser "digitada" quando for exibida. */
    public void animate(int position) {
        animatePosition = position;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        ChatTurn turn = turns.get(position);
        boolean user = turn.isUser();
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) h.bubble.getLayoutParams();
        lp.gravity = user ? Gravity.END : Gravity.START;
        h.bubble.setLayoutParams(lp);
        h.bubble.setBackgroundResource(user ? R.drawable.bg_bubble_user : R.drawable.bg_bubble_jarvis);
        h.bubble.setTextColor(h.bubble.getContext().getColor(user ? R.color.aegis_gold : R.color.aegis_text));

        if (position == animatePosition && !user) {
            animatePosition = -1;
            Hud.typewriter(h.bubble, turn.text);
        } else {
            h.bubble.setText(turn.text);
        }
    }

    @Override
    public int getItemCount() {
        return turns.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView bubble;

        Holder(View v) {
            super(v);
            bubble = v.findViewById(R.id.bubble);
        }
    }
}
