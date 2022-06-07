package tk.donkeyblaster.ftxpricenotification;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TickersAdapter extends RecyclerView.Adapter<TickersAdapter.ViewHolder> {
    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public Button deleteButton;
        public Button editButton;

        public ViewHolder(View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.ticker);
            deleteButton = itemView.findViewById(R.id.delete_button);
            editButton = itemView.findViewById(R.id.edit_button);

            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Ticker.tickers.remove(getAdapterPosition());
                    TickersAdapter.super.notifyItemRemoved(getAdapterPosition());
                }
            });

            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(itemView.getContext(), EditTickerActivity.class);
                    intent.putExtra("index", getAdapterPosition());
                    itemView.getContext().startActivity(intent);

                    TickersAdapter.super.notifyItemChanged(getAdapterPosition());
                }
            });
        }
    }

    private List<Ticker> mTickers;

    public TickersAdapter(List<Ticker> tickers) {
        this.mTickers = tickers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.item_ticker, parent, false);

        // Return a new holder instance
        return new ViewHolder(contactView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the data model based on position
        Ticker ticker = mTickers.get(position);

        // Set item views based on your views and data model
        TextView textView = holder.nameTextView;
        textView.setText(ticker.getTicker());
        Button deleteButton = holder.deleteButton;
        Button editButton = holder.editButton;

    }

    @Override
    public int getItemCount() {
        return mTickers.size();
    }
}