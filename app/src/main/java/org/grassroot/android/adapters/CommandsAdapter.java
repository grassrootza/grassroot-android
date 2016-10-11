package org.grassroot.android.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import org.grassroot.android.R;
import org.grassroot.android.models.Command;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by paballo on 2016/09/20.
 */
public class CommandsAdapter extends ArrayAdapter<Command> {

    final List<Command> commands;
    List<Command> filteredCommands = new ArrayList<>();

    public CommandsAdapter(Context context, List<Command> commands) {
        super(context,0,commands);
        this.commands  =commands;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Command command = filteredCommands.get(position);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        convertView = inflater.inflate(R.layout.row_chat_command,parent,false);

        TextView txt_command =  (TextView) convertView.findViewById(R.id.txt_command);
        TextView txt_hint = (TextView)convertView.findViewById(R.id.txt_command_hint);
        TextView txt_description = (TextView)convertView.findViewById(R.id.txt_command_description);

        txt_command.setText(command.getCommand());
        txt_hint.setText(command.getHint());
        txt_description.setText(command.getDescrption());

        return  convertView;
    }


    @Override
    public int getCount() {
        return filteredCommands.size();
    }

    @Override
    public Filter getFilter() {
        return new CommandFilter(this, commands);
    }


    private class CommandFilter extends Filter{

        CommandsAdapter commandsAdapter;
        List<Command> originalList;
        List<Command> filteredList;


        public CommandFilter(CommandsAdapter commandsAdapter, List<Command> originalList){
            super();
            this.commandsAdapter = commandsAdapter;
            this.originalList = originalList;
            this.filteredList = new ArrayList<>();
        }


        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {

            filteredList.clear();
            final FilterResults results = new FilterResults();

            if (charSequence == null || charSequence.length() == 0) {
                filteredList.addAll(originalList);
            } else {
                final String filterPattern = charSequence.toString().toLowerCase().trim();

                for (final Command command : originalList) {
                    if (command.getCommand().toLowerCase().startsWith(filterPattern)) {
                        filteredList.add(command);
                    }
                }
            }
            results.values = filteredList;
            results.count = filteredList.size();

            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            commandsAdapter.filteredCommands.clear();
            commandsAdapter.filteredCommands.addAll((List) filterResults.values);
            commandsAdapter.notifyDataSetChanged();
        }
    }
}
