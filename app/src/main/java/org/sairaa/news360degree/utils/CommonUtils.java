package org.sairaa.news360degree.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import org.sairaa.news360degree.MainActivity;
import org.sairaa.news360degree.R;
import org.sairaa.news360degree.api.ApiUtils;
import org.sairaa.news360degree.api.NewsApi;
import org.sairaa.news360degree.db.News;
import org.sairaa.news360degree.db.NewsDatabase;
import org.sairaa.news360degree.model.NewsList;
import org.sairaa.news360degree.service.BackgroundService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommonUtils {
    private Context context;
    private DialogAction dialogAction;

    public CommonUtils(Context context) {
        this.context = context;
        dialogAction = new DialogAction(context);
    }

    //Retrieve top headline news and insert it into room and notifies user on new news arrival
    public void insertNewsToDb(final Executor executor, final String category, final String APIKEY) {
        //get local country name
        String countryName = context.getResources().getConfiguration().locale.getDisplayCountry();
        //get the country code for retrival of news in respective country
        String countryCode = getCountryCode(countryName);
        final NewsDatabase mDb = NewsDatabase.getsInstance(context);
        NewsApi newsApi = ApiUtils.getNewsApi();

        dialogAction.showDialog(context.getString(R.string.app_name),context.getString(R.string.retrieve));
        //make retrofit call to retrieve category news of the respective country
        newsApi.getTopHeadLine(countryCode,APIKEY).enqueue(new Callback<NewsList>() {
            @Override
            public void onResponse(Call<NewsList> call, Response<NewsList> response) {
                final NewsList newsList = response.body();
                for(int i =0;i<newsList.getNewsDataList().size();i++){
                    final int position = i;
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            //check whether same news object available or not
                            //if not available insert that news object to room database
                            List<News> newsL = mDb.newsDao().getSingleNews(newsList.getNewsDataList().get(position).getTitle());
                            if(newsL.isEmpty()){
                                // create news object to insert it into room
                                News news = new News(newsList.getNewsDataList().get(position).getAuthor() == null ? context.getString(R.string.newsApi) :newsList.getNewsDataList().get(position).getAuthor(),
                                        newsList.getNewsDataList().get(position).getTitle() == null ? "" : newsList.getNewsDataList().get(position).getTitle(),
                                        newsList.getNewsDataList().get(position).getDescription() == null? "" :newsList.getNewsDataList().get(position).getDescription(),
                                        newsList.getNewsDataList().get(position).getUrl() == null? "":newsList.getNewsDataList().get(position).getUrl(),
                                        newsList.getNewsDataList().get(position).getUrlToImage()== null ? "":newsList.getNewsDataList().get(position).getUrlToImage(),
                                        newsList.getNewsDataList().get(position).getPublishedAt()== null? "":newsList.getNewsDataList().get(position).getPublishedAt(),
                                        getBookMark(category));
                                try {
                                    //insert news object to room
                                    insertNewsToDbLocal(news,mDb);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
                dialogAction.hideDialog();
            }

            @Override
            public void onFailure(Call<NewsList> call, Throwable t) {
                dialogAction.hideDialog();
            }
        });
    }

    private void insertNewsToDbLocal(final News news, final NewsDatabase mDb) throws IOException {
        if(!news.getUrlToImage().equals("")){
            //insert image to internal storage and get the path and set it to news object
            String path = uploadImageToInternalStorage(news.getUrlToImage());
            news.setUrlToImage(path);
        }
        //format the date and time and set it to news object
        String dateTime = CommonUtils.getDate(news.getPublishedAt()).concat(" ").concat(CommonUtils.getTime(news.getPublishedAt()));
        news.setPublishedAt(dateTime);

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                mDb.newsDao().insert(news);
            }
        });
    }

    public String uploadImageToInternalStorage(String urlToImage) {
        Bitmap bitmap = null;
        bitmap = getBitmapFromUrl(urlToImage);
        if(bitmap == null)
            return "";
        return saveImage(context,bitmap);
//        return null;
    }

    private String saveImage(Context context, Bitmap bitmap) {
        int uniqueInteger = (int) ((new Date().getTime()) % Integer.MAX_VALUE);
        String filename = String.valueOf(uniqueInteger)+".jpg";
        File file = new File(context.getFilesDir(), filename);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file.getAbsolutePath();
    }

    private Bitmap getBitmapFromUrl(String urlToImage) {
        URLConnection connection = null;
        Bitmap bitmap = null;
        try {
            connection = new URL(urlToImage).openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (connection != null) {
                bitmap = BitmapFactory.decodeStream((InputStream)connection.getContent(), null, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        return bitmap;
    }

    public static String getDate(String dateString) {

        try{
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
            Date date = format1.parse(dateString);
            DateFormat sdf = new SimpleDateFormat("MMM d yyyy");
            return sdf.format(date);
        }
        catch(Exception ex) {
            ex.printStackTrace();
            return "xx";
        }
    }

    public static String getTime(String dateString) {

        try{
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss'Z'");
            Date date = format1.parse(dateString);
            DateFormat sdf = new SimpleDateFormat("h:mm a");
            Date netDate = (date);
            return sdf.format(netDate);
        }
        catch(Exception ex) {
            ex.printStackTrace();
            return "xx";
        }
    }


    public static long getRandomNumber() {
        long x = (long) ((Math.random() * ((100000 - 0) + 1)) + 0);
        return x;
    }

    public static void showNotification(Context myService, String s){
        int uniqueInteger = 0;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(myService,s);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setSmallIcon(R.drawable.news360);
            builder.setLargeIcon(BitmapFactory.decodeResource(myService.getResources(), R.drawable.news360));
            builder.setColor(myService.getResources().getColor(R.color.colorAccent));
        } else {
            builder.setSmallIcon(R.drawable.news360);
        }
        builder.setContentTitle(myService.getString(R.string.app_name));
        builder.setContentText(s);
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(uri);
        builder.setAutoCancel(true);
        Intent intent = new Intent(myService, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(myService);
        stackBuilder.addNextIntent(intent);
        uniqueInteger = (int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager) myService.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(uniqueInteger, builder.build());

    }

    public String getCountryCode(String countryName) {
        if (countryName.equals(context.getString(R.string.india))) {
            return context.getString(R.string.india_code);

        }
        return context.getString(R.string.india_code);
    }

    public int getBookMark(String category) {
        if(category.equals(context.getString(R.string.business_cat))){
            return 2;
        }else if(category.equals(context.getString(R.string.entertainment_cat))){
            return 3;
        }else if(category.equals(context.getString(R.string.health_cat))){
            return 4;
        }else if(category.equals(context.getString(R.string.science_cat))){
            return 5;
        }else if(category.equals(context.getString(R.string.sports_cat))){
            return 6;
        }else if(category.equals(context.getString(R.string.technology_cat))){
            return 7;
        }else
            return 1;
    }
}
