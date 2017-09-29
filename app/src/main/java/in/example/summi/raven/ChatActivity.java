package in.example.summi.raven;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class ChatActivity extends AppCompatActivity {

    private String mChatuser,mChatusername;
    private Toolbar mtoolbar;
    private DatabaseReference mRootref;

    private FirebaseAuth mAuth;

    private int refreshed = 0;

    private TextView username_tv,lastseen_tv;
    private CircleImageView muserdp;

    private SwipeRefreshLayout mSwipeRefresh;
    
    
    private ImageButton addbut,sendbut;
    private EditText messagetext;

    private LinearLayout emessage_view;

    private RecyclerView mmessageslist;

    private String publicKeyString;
    private String publicKeyStringOwn;
    private String privateKeyString;

    private static final int MESSAGES_TO_LOAD = 40;
    private int current_page = 1;
    private String Uid;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter mAdapter;

    private int crypted = 0;

    public String MY_PREFS_NAME = "socio_prefs";

    private StorageReference mStorageRef;
    private DatabaseReference pubkeyref;


    @Override
    protected void onPause() {
        super.onPause();

        FirebaseUser curr_user = mAuth.getCurrentUser();

        if (curr_user != null) {
            mRootref.child("Users").child(curr_user.getUid()).child("online").setValue(ServerValue.TIMESTAMP);
        }

    }


    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser curr_user = mAuth.getCurrentUser();

        if (curr_user != null) {
            mRootref.child("Users").child(curr_user.getUid()).child("online").setValue("true");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        mtoolbar = (Toolbar) findViewById(R.id.chat_appbar);
        setSupportActionBar(mtoolbar);

        mAuth = FirebaseAuth.getInstance();
        Uid = mAuth.getCurrentUser().getUid();

        mmessageslist = (RecyclerView) findViewById(R.id.messages_rv);
        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);


        mmessageslist.setHasFixedSize(true);
        mmessageslist.setLayoutManager(linearLayoutManager);


        mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_layout_chat);
        emessage_view = (LinearLayout) findViewById(R.id.encrypted_message);

        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        mChatuser = getIntent().getStringExtra("from_user_id");
        mChatusername = getIntent().getStringExtra("from_username");

        pubkeyref = FirebaseDatabase.getInstance().getReference().child("PubKey");

        mRootref = FirebaseDatabase.getInstance().getReference();
        mRootref.child("Users").child(mAuth.getCurrentUser().getUid()).child("online").setValue("true");

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionbar_view = inflater.inflate(R.layout.chat_custom_bar,null);

        actionBar.setCustomView(actionbar_view);





        mRootref.child("messages").child(Uid).child(mChatuser).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int count = (int) dataSnapshot.getChildrenCount();
                if(count<10){
                    emessage_view.setVisibility(View.VISIBLE);
                }else{
                    emessage_view.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });




        lastseen_tv = (TextView) findViewById(R.id.chat_user_last_seen);
        username_tv = (TextView) findViewById(R.id.chat_user_name);
        muserdp = (CircleImageView) findViewById(R.id.chat_user_dp);
        
        addbut = (ImageButton) findViewById(R.id.chat_add_but);
        sendbut = (ImageButton) findViewById(R.id.chat_send_but);
        messagetext = (EditText) findViewById(R.id.chat_message_text); 
        

        username_tv.setText(mChatusername);
        lastseen_tv.setVisibility(View.GONE);
        ;

        muserdp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent profIntent = new Intent(ChatActivity.this,ProfileActivity.class);
                profIntent.putExtra("from_user_id",mChatuser);
                startActivity(profIntent);
            }
        });


        pubkeyref.child(mChatuser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.hasChild("pub")) {
                    publicKeyString = dataSnapshot.child("pub").getValue().toString();
                    crypted = 1;
                }

            }


            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        pubkeyref.child(Uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(dataSnapshot.hasChild("pub")) {
                    publicKeyStringOwn = dataSnapshot.child("pub").getValue().toString();
                }

            }


            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        pubkeyref.keepSynced(true);
            loadmessages();

        mRootref.child("Users").child(mChatuser).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String online = dataSnapshot.child("online").getValue().toString();
                String image = dataSnapshot.child("thumb_image").getValue().toString();

                if(online.equals("true")){
                    lastseen_tv.setText("online");
                    lastseen_tv.setVisibility(View.VISIBLE);

                }else {
                    GetTimeAgo gta = new GetTimeAgo();
                    long lastime = Long.parseLong(online);
                    String lastseentime = gta.getTimeAgo(lastime);
                    lastseen_tv.setText(lastseentime);
                    lastseen_tv.setVisibility(View.VISIBLE);
                }

                if(image!=null){
                    Picasso.with(ChatActivity.this).load(image)
                            .placeholder(R.drawable.avatar)
                            .into(muserdp);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });




        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                current_page+=1;
                refreshed=1;
                loadmessages();



            }
        });
        
        
        sendbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendmessage();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (InvalidKeySpecException e) {
                    e.printStackTrace();
                }
            }
            
        });

        addbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendImage();
            }
        });
    }



    private void sendImage() {

        CropImage.activity()
                .start(ChatActivity.this);

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                Uri resultUri = result.getUri();

                uploadImage(resultUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private void uploadImage(Uri resultUri) {

        Random random = new Random();

        Map chatusermap = new HashMap();
        chatusermap.put("Chat/"+mChatuser+"/"
                +Uid+"/timestamp",ServerValue.TIMESTAMP);
        chatusermap.put("Chat/"+Uid+"/"+mChatuser+"/timestamp",ServerValue.TIMESTAMP);

        mRootref.updateChildren(chatusermap, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if(databaseError!=null){
                    Log.d("CHAT_LOG",databaseError.getMessage().toString());
                }
            }
        });

        StorageReference filepath = mStorageRef.child("image_messages").child(Uid).child(mChatuser).child(random.nextInt(10000000)+".jpg");
        File image_file = new File(resultUri.getPath());

        Bitmap compressedImageBitmap = new Compressor(this)
                .setMaxHeight(400)
                .setMaxWidth(400)
                .setQuality(2)
                .compressToBitmap(image_file);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        compressedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        final byte[] image_data = baos.toByteArray();
        UploadTask uploadTask = filepath.putBytes(image_data);

        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                @SuppressWarnings("VisibleForTests") String download_url = task.getResult().getDownloadUrl().toString();
                if(task.isSuccessful()){
                    String curruserref = "messages/"+Uid+"/"+mChatuser;
                    String chatuserref = "messages/"+mChatuser+"/"+Uid;

                    DatabaseReference usermessage_push = mRootref.child("messages")
                            .child(Uid).child(mChatuser).push();

                    String push_id = usermessage_push.getKey();

                    Map messagemap = new HashMap();
                    messagemap.put("message",download_url);
                    messagemap.put("seen",false);
                    messagemap.put("type","image");
                    messagemap.put("time",ServerValue.TIMESTAMP);
                    messagemap.put("from",Uid);


                    Map messageUsermap = new HashMap();
                    messageUsermap.put(curruserref+"/"+push_id,messagemap);
                    messageUsermap.put(chatuserref+"/"+push_id,messagemap);


                    mRootref.updateChildren(messageUsermap, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            if(databaseError!=null){
                                Log.d("CHAT_LOG",databaseError.getMessage().toString());
                            }
                        }
                    });
                }
            }
        });
    }

    private void loadmessages() {

        SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        privateKeyString = prefs.getString("private_key"+mAuth.getCurrentUser().getUid(), null);


        final String Uid = mAuth.getCurrentUser().getUid();
        final Query mref = mRootref.child("messages").child(Uid)
               .child(mChatuser).limitToLast(MESSAGES_TO_LOAD*current_page);


        final FirebaseRecyclerAdapter<Messages,mViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Messages, mViewHolder>(
                Messages.class,
                R.layout.message_item,
                mViewHolder.class,
                mref
        ) {


            @Override
            protected void populateViewHolder(final mViewHolder viewHolder, final Messages model, final int position) {

                viewHolder.initialize();
                RSAAlgo rsaAlgo = new RSAAlgo();
                String message = model.getMessage();
                String type = model.getType();

                String decryptedmessage = message;
                if(crypted==1 && type.equals("text")) {

                    if(decryptedmessage.length() > 100 && !decryptedmessage.contains(" ")) {

                        try {
                            decryptedmessage = rsaAlgo.Decrypt(message, privateKeyString);
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (NoSuchPaddingException e) {
                            e.printStackTrace();
                        } catch (InvalidKeyException e) {
                            e.printStackTrace();
                        } catch (IllegalBlockSizeException e) {
                            e.printStackTrace();
                        } catch (BadPaddingException e) {
                            e.printStackTrace();
                        } catch (InvalidKeySpecException e) {
                            e.printStackTrace();
                        } catch (NumberFormatException e){
                            e.printStackTrace();
                        }
                    }
                }


                viewHolder.setMessage(decryptedmessage,type,getApplicationContext());
                viewHolder.setTime(model.getTime(),model.getType());
                viewHolder.setPosition(Uid,model.getFrom());
                viewHolder.setType(model.getType());



                viewHolder.lview.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        CharSequence options[] = new CharSequence[]{"Copy","Delete"};


                       final String message_key = getRef(position).getKey();
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ChatActivity.this);


                        if(model.getType().equals("image")){
                            alertDialogBuilder.setTitle("Delete Image?");
                            alertDialogBuilder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    String curruserref = "messages/" + Uid + "/" + mChatuser;
                                    String chatuserref = "messages/" + mChatuser + "/" + Uid;

                                    Map delmap = new HashMap();
                                    delmap.put(curruserref + "/" + message_key, null);
                                    delmap.put(chatuserref + "/" + message_key, null);

                                    mRootref.updateChildren(delmap).addOnSuccessListener(new OnSuccessListener() {
                                        @Override
                                        public void onSuccess(Object o) {
                                            Toast.makeText(ChatActivity.this, "Message Deleted", Toast.LENGTH_SHORT).show();
                                        }
                                    });


                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).show();
                        }else {
                            alertDialogBuilder.setTitle(viewHolder.text_tv.getText().toString());
                            alertDialogBuilder.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    if(which==0){
                                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                        ClipData clip = ClipData.newPlainText("TEXT", viewHolder.text_tv.getText().toString());
                                        clipboard.setPrimaryClip(clip);
                                        Toast.makeText(ChatActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                                    }
                                    if(which==1){
                                        String curruserref = "messages/" + Uid + "/" + mChatuser;
                                        String chatuserref = "messages/" + mChatuser + "/" + Uid;

                                        Map delmap = new HashMap();
                                        delmap.put(curruserref + "/" + message_key, null);
                                        delmap.put(chatuserref + "/" + message_key, null);

                                        mRootref.updateChildren(delmap).addOnSuccessListener(new OnSuccessListener() {
                                            @Override
                                            public void onSuccess(Object o) {
                                                Toast.makeText(ChatActivity.this, "Message Deleted", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                }
                            });
                            alertDialogBuilder.show();
                        }

                        return true;
                    }
                });

            }
        };

        mmessageslist.setAdapter(firebaseRecyclerAdapter);
        mmessageslist.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if ( bottom < oldBottom) {

                    mmessageslist.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mmessageslist.scrollToPosition(firebaseRecyclerAdapter.getItemCount());
                        }
                    }, 100);
                }


            }
        });


            firebaseRecyclerAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onItemRangeInserted(int positionStart, int itemCount) {
                    super.onItemRangeInserted(positionStart, itemCount);

                    mmessageslist.scrollToPosition(positionStart);
                    if(refreshed==1) {
                        mmessageslist.scrollToPosition(itemCount - 1);
                    }
                    linearLayoutManager.setReverseLayout(false);
                    linearLayoutManager.setStackFromEnd(true);

                }
            });


        mSwipeRefresh.setRefreshing(false);
    }
    public static class mViewHolder extends RecyclerView.ViewHolder {


        View mView;
        TextView mtime_tv, text_tv, itime_tv;
        ImageView imageview;
        LinearLayout lview, lmessage, limage;


        final LinearLayout.LayoutParams params;


        public mViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
            params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        public void initialize() {

            mtime_tv = (TextView) mView.findViewById(R.id.message_item_time);
            itime_tv = (TextView) mView.findViewById(R.id.image_item_time);

            text_tv = (TextView) mView.findViewById(R.id.message_item_text);
            imageview = (ImageView) mView.findViewById(R.id.chat_image);

            lview = (LinearLayout) mView.findViewById(R.id.message_item_view);
            lmessage = (LinearLayout) mView.findViewById(R.id.message_item_layout);
            limage = (LinearLayout) mView.findViewById(R.id.image_item_layout);

        }


        public void setType(String type) {

            if (type.equals("text")) {
                params.height = 0;
                lmessage.setVisibility(View.VISIBLE);

            } else if (type.equals("image")) {
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                lmessage.setVisibility(View.GONE);
            }

            limage.setLayoutParams(params);

        }

        public void setTime(long time, String type) {

            GetTimeAgo gta = new GetTimeAgo();
            String lastseentime = gta.getTimeAgo(time);


            if (type.equals("text")) {
                mtime_tv.setText(lastseentime);
                itime_tv.setVisibility(View.INVISIBLE);
                mtime_tv.setVisibility(View.VISIBLE);
            } else if (type.equals("image")) {
                itime_tv.setText(lastseentime);
                itime_tv.setVisibility(View.VISIBLE);
                mtime_tv.setVisibility(View.INVISIBLE);
            }
        }

        public void setMessage(String text, String type, Context ctx) {

            if (type.equals("text")) {
                text_tv.setText(text);

            } else if (type.equals("image")) {
                Picasso.with(ctx).load(text).placeholder(R.drawable.image_load_anim)
                        .into(imageview);
            }
        }

        public void setPosition(String Uid, String from) {

            if (Uid.equals(from)) {
                lview.setGravity(Gravity.END);
                lmessage.setBackgroundResource(R.drawable.message_bg_light);
                limage.setBackgroundResource(R.drawable.message_bg_light);
                text_tv.setTextColor(Color.WHITE);
                itime_tv.setTextColor(Color.DKGRAY);
                mtime_tv.setTextColor(Color.DKGRAY);
            } else {
                lview.setGravity(Gravity.START);
                itime_tv.setTextColor(Color.LTGRAY);
                mtime_tv.setTextColor(Color.LTGRAY);
                lmessage.setBackgroundResource(R.drawable.message_bg_dark);
                limage.setBackgroundResource(R.drawable.message_bg_dark);
                text_tv.setTextColor(Color.WHITE);
            }
        }

    }
    private void sendmessage() throws IllegalBlockSizeException, InvalidKeyException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException {


        refreshed = 0;
        final String message = messagetext.getText().toString();
        messagetext.setText("");
        final String Uid = mAuth.getCurrentUser().getUid();


        RSAAlgo rsaAlgo = new RSAAlgo();
        String em = message;
        String emown = message;
        if(crypted==1) {
            em = rsaAlgo.Encrypt(message, publicKeyString);
            emown = rsaAlgo.Encrypt(message,publicKeyStringOwn);
        }
        final String encryptedmessage = em;
        final String encryptedmessageown = emown;


        Map chatusermap = new HashMap();
        chatusermap.put("Chat/"+mChatuser+"/"
                +Uid+"/timestamp",ServerValue.TIMESTAMP);
        chatusermap.put("Chat/"+Uid+"/"+mChatuser+"/timestamp",ServerValue.TIMESTAMP);

        mRootref.updateChildren(chatusermap, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if(databaseError!=null){
                    Log.d("CHAT_LOG",databaseError.getMessage().toString());
                }
            }
        });

        if(!TextUtils.isEmpty(message)){

            mRootref.child("Chat").child(mChatuser).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(!dataSnapshot.hasChild(Uid)){
                        Map chataddmap = new HashMap();
                        chataddmap.put("seen","false");
                        chataddmap.put("timestamp",ServerValue.TIMESTAMP);

                        Map chatusermap = new HashMap();
                        chatusermap.put("Chat/"+mChatuser+"/"
                                +Uid,chataddmap);

                        mRootref.updateChildren(chatusermap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(databaseError!=null){
                                    Log.d("CHAT_LOG",databaseError.getMessage().toString());
                                }
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });






            String curruserref = "messages/"+Uid+"/"+mChatuser;
            String chatuserref = "messages/"+mChatuser+"/"+Uid;

            DatabaseReference usermessage_push = mRootref.child("messages")
                    .child(Uid).child(mChatuser).push();

            String push_id = usermessage_push.getKey();

            Map messagemap = new HashMap();
            messagemap.put("message",encryptedmessage);
            messagemap.put("seen",false);
            messagemap.put("type","text");
            messagemap.put("time",ServerValue.TIMESTAMP);
            messagemap.put("from",Uid);

            Map messagemapOwn = new HashMap();
            messagemapOwn.put("message",encryptedmessageown);
            messagemapOwn.put("seen",false);
            messagemapOwn.put("type","text");
            messagemapOwn.put("time",ServerValue.TIMESTAMP);
            messagemapOwn.put("from",Uid);

            Map messageUsermap = new HashMap();
            messageUsermap.put(curruserref+"/"+push_id,messagemapOwn);
            messageUsermap.put(chatuserref+"/"+push_id,messagemap);


            mRootref.updateChildren(messageUsermap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(final DatabaseError databaseError, DatabaseReference databaseReference) {

                    if(databaseError!=null){
                        Log.d("CHAT_LOG",databaseError.getMessage().toString());
                    }else{


                        mRootref.child("Users").child(mChatuser).child("online").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String online = dataSnapshot.getValue().toString();

                                if(!online.equals("true")){

                                    Map notimap = new HashMap();
                                    notimap.put("from",Uid);
                                    notimap.put("type","message");
                                    notimap.put("message",message);

                                    mRootref.child("MessageNoti").child(mChatuser).push().setValue(notimap)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                }
                                            });
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });


                    }
                }
            });
        }

    }
}
