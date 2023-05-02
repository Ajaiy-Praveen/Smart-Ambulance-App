package me.ajaybala.ambulanceapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;



import java.util.ArrayList;

import me.ajaybala.ambulanceapp.R;
import me.ajaybala.ambulanceapp.models.PlaceDetail;


public class PlaceDetailsAdapter extends RecyclerView.Adapter<PlaceDetailsAdapter.ContributionViewHolder>{

    private ArrayList<PlaceDetail> PlaceDetail;
    private Context context;
    private final OnItemClickListener onItemClickListener;

    public PlaceDetailsAdapter(Context context, ArrayList<PlaceDetail> PlaceDetail, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.PlaceDetail = PlaceDetail;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public PlaceDetailsAdapter.ContributionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem;
        listItem = layoutInflater.inflate(R.layout.place_row_item, parent, false);
        return new PlaceDetailsAdapter.ContributionViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceDetailsAdapter.ContributionViewHolder holder, int position) {
        final PlaceDetail placeDetail = (PlaceDetail) PlaceDetail.get(position);
        holder.placeTV1.setText(placeDetail.getPrimaryText());
        holder.placeTV2.setText(placeDetail.getSecondaryText());
        if(placeDetail.isRecent()){
            holder.placeIV.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_refresh_black_24dp));
        }else{
            holder.placeIV.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_map_pin));
        }
        holder.containerRL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.onItemClick(placeDetail);
            }
        });

    }

    @Override
    public int getItemCount() {
        return PlaceDetail.size();
    }

    public class ContributionViewHolder extends RecyclerView.ViewHolder {
        public TextView placeTV1, placeTV2;
        public RelativeLayout containerRL;
        public ImageView placeIV;

        public ContributionViewHolder(View convertView) {
            super(convertView);

            placeTV1 = convertView.findViewById(R.id.placeTV1);
            placeTV2 = convertView.findViewById(R.id.placeTV2);
            containerRL = convertView.findViewById(R.id.containerRL);
            placeIV = convertView.findViewById(R.id.placeIV);

        }
    }

    public interface OnItemClickListener {
        void onItemClick(PlaceDetail item);
    }
}
