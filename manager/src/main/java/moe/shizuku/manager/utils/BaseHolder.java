package moe.shizuku.manager.utils;

import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

public class BaseHolder<V> extends RecyclerView.ViewHolder {

    public BaseHolder(View itemView) {
        super(itemView);
    }
    public void bind(V data, int position){}
    public void event(){}

}
