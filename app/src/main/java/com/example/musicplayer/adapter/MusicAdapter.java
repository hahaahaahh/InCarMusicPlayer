package com.example.musicplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicplayer.R;
import com.example.musicplayer.model.MusicItem;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {
    private List<MusicItem> musicList;
    private Context context;
    private int currentSongIndex = 0; // Track current song position

    // Interface for item click
    public interface OnItemClickListener {
        void onItemClick(int position);
    }
    private OnItemClickListener onItemClickListener;
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public MusicAdapter(Context context, List<MusicItem> musicList) {
        this.context = context;
        this.musicList = musicList;
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_music, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        MusicItem item = musicList.get(position);
        holder.title.setText(item.getTitle());
        holder.artist.setText(item.getArtist());
        holder.album.setText(item.getAlbum());
        holder.duration.setText(item.getDuration());
        holder.albumCover.setImageResource(R.drawable.right_cover);
        holder.albumCover.setContentDescription("Album cover for " + item.getTitle());
        holder.itemView.setFocusable(true);
        holder.itemView.setContentDescription(item.getTitle() + ", " + item.getArtist() + ", " + item.getAlbum() + ", " + item.getDuration());
        
        // Highlight current song with special color
        if (position == currentSongIndex) {
            holder.title.setTextColor(0xFF2FEFE4); // #ff2fefe4
            holder.artist.setTextColor(0xFF2FEFE4); // #ff2fefe4
            holder.album.setTextColor(0xFF2FEFE4); // #ff2fefe4
            holder.duration.setTextColor(0xFF2FEFE4); // #ff2fefe4
        } else {
            holder.title.setTextColor(0xFF000000); // Black
            holder.artist.setTextColor(0xFF000000); // Black
            holder.album.setTextColor(0xFF000000); // Black
            holder.duration.setTextColor(0xFF000000); // Black
        }

        // Set click listener
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    // Update the current song index and refresh the adapter
    public void updateCurrentSongIndex(int newIndex) {
        this.currentSongIndex = newIndex;
        notifyDataSetChanged();
    }

    public static class MusicViewHolder extends RecyclerView.ViewHolder {
        ImageView albumCover;
        TextView title, artist, album, duration;
        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            albumCover = itemView.findViewById(R.id.iv_album_cover);
            title = itemView.findViewById(R.id.tv_track_title);
            artist = itemView.findViewById(R.id.tv_track_artist);
            album = itemView.findViewById(R.id.tv_track_album);
            duration = itemView.findViewById(R.id.tv_track_duration);
        }
    }
}
