package org.mozilla.kitfox;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 *  Chat array adapter
 *
 *  Inserts chat messages into a scrollable list view.
 */
public class ChatArrayAdapter extends ArrayAdapter<ChatMessage> {

    private TextView messageText;
    private List<ChatMessage> chatMessageList = new ArrayList<ChatMessage>();
    private Context context;

    public ChatArrayAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        this.context = context;
    }

    public void add(ChatMessage message) {
        chatMessageList.add(message);
        super.add(message);
    }

    public int getCount() {
        return this.chatMessageList.size();
    }

    public ChatMessage getItem(int index) {
        return this.chatMessageList.get(index);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ChatMessage chatMessageObject = getItem(position);
        View row = convertView;
        LayoutInflater inflater = (LayoutInflater)
                this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (chatMessageObject.direction) {
            row = inflater.inflate(R.layout.incoming_message, parent, false);
        } else {
            row = inflater.inflate(R.layout.outgoing_message, parent, false);
        }
        messageText = (TextView) row.findViewById(R.id.message_text);
        messageText.setText(chatMessageObject.messageText);
        return row;
    }

}
