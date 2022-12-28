package com.yuuna.pdfviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.Holder> {

    private ArrayList<SliderItems> sliderModelArrayList;
    private Context context;

    public SliderAdapter(ArrayList<SliderItems> sliderItemsArrayList, Context context) {
        this.sliderModelArrayList = sliderItemsArrayList;
        this.context = context;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.slider_items, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        SliderItems sliderItems = sliderModelArrayList.get(position);

        try {
            FileInputStream fileInputStream = context.openFileInput(sliderItems.getBitmap());
            Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream);
            fileInputStream.close();
            holder.ssPDF.setImage(ImageSource.bitmap(bitmap));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return sliderModelArrayList.size();
    }

    public class Holder extends RecyclerView.ViewHolder{
        private SubsamplingScaleImageView ssPDF;

        public Holder(View view) {
            super(view);
            ssPDF = view.findViewById(R.id.pdf_image);
        }
    }
}