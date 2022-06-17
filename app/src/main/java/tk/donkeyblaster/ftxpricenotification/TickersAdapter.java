package tk.donkeyblaster.ftxpricenotification;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
        public TextView tickerTextView;
        public TextView positionDataTextView;
        public Button deleteButton;
        public Button editButton;

        public ViewHolder(View itemView) {
            super(itemView);

            tickerTextView = itemView.findViewById(R.id.ticker);
            positionDataTextView = itemView.findViewById(R.id.positionData);
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

            // Uncomment to have edit dialogue open on entry tap
//            itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    Intent intent = new Intent(itemView.getContext(), EditTickerActivity.class);
//                    intent.putExtra("index", getAdapterPosition());
//                    itemView.getContext().startActivity(intent);
//
//                    TickersAdapter.super.notifyItemChanged(getAdapterPosition());
//                }
//            });
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

        // Set item ticker text
        TextView tickerTV = holder.tickerTextView;
        tickerTV.setText(ticker.getTicker());

        // Set possible position data, returns if positionSize == 0
        TextView positionDataTV = holder.positionDataTextView;
        float positionSize = ticker.getPositionSize();
        if (positionSize == 0) {
            String toSet = "No position\ndata available";
            positionDataTV.setText(toSet);
            return;
        }

        float entryPrice = ticker.getEntryPrice();
        String side = (positionSize > 0) ? "Long" : "Short";
        String toSet = side + " " + positionSize + "\n" + "Entry " + entryPrice;
        positionDataTV.setText(toSet);
        positionDataTV.setTextColor((side.equals("Long")) ? Color.parseColor("#50dd50") : Color.parseColor("#ff4343"));
    }



    @Override
    public int getItemCount() {
        return mTickers.size();
    }
}