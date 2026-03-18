package com.example.edubridge.shared.messaging;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ViewHolder> {

    private final List<Contact> contacts;
    private final OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(Contact contact);
    }

    public ContactAdapter(List<Contact> contacts, OnContactClickListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_contact, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact contact = contacts.get(position);

        // Avatar: first letter of name
        String name = contact.getFullName();
        holder.avatar.setText(name != null && !name.isEmpty()
                ? String.valueOf(name.charAt(0)).toUpperCase() : "?");

        holder.contactName.setText(name);

        // Last message preview
        String lastMsg = contact.getLastMessage();
        if (lastMsg != null && !lastMsg.isEmpty()) {
            holder.lastMessage.setText(lastMsg);
            holder.lastMessage.setVisibility(View.VISIBLE);
        } else {
            holder.lastMessage.setVisibility(View.GONE);
        }

        // Time
        if (contact.getLastMessageTime() > 0) {
            holder.timeText.setText(TimeFormatter.formatTimestamp(contact.getLastMessageTime()));
            holder.timeText.setVisibility(View.VISIBLE);
        } else {
            holder.timeText.setVisibility(View.GONE);
        }

        // Unread badge
        if (contact.getUnreadCount() > 0) {
            holder.unreadBadge.setText(String.valueOf(contact.getUnreadCount()));
            holder.unreadBadge.setVisibility(View.VISIBLE);
        } else {
            holder.unreadBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onContactClick(contact));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView avatar, contactName, lastMessage, timeText, unreadBadge;

        ViewHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            contactName = itemView.findViewById(R.id.contact_name);
            lastMessage = itemView.findViewById(R.id.last_message);
            timeText = itemView.findViewById(R.id.time_text);
            unreadBadge = itemView.findViewById(R.id.unread_badge);
        }
    }
}
