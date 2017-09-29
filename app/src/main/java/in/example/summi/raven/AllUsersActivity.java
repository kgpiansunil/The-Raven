package in.example.summi.raven;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class AllUsersActivity extends AppCompatActivity {

    private RecyclerView allUsersRv;
    private DatabaseReference mDatabaseUsersReference;
    FirebaseRecyclerAdapter<Users, AllUsersViewHolder> firebaseRecyclerAdapter;

    private RelativeLayout mRelLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users);

        Toolbar toolbar = (Toolbar) findViewById(R.id.all_users_appbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("All Users");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRelLayout = (RelativeLayout) findViewById(R.id.all_users_rel_layout);

        mDatabaseUsersReference = FirebaseDatabase.getInstance().getReference().child("Users");
        allUsersRv = (RecyclerView) findViewById(R.id.rv_all_users);
        allUsersRv.setHasFixedSize(true);
        allUsersRv.setLayoutManager(new LinearLayoutManager(this));

        firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Users, AllUsersViewHolder>(
                Users.class, R.layout.user_item, AllUsersViewHolder.class, mDatabaseUsersReference
        ) {
            @Override
            protected void populateViewHolder(AllUsersViewHolder viewHolder, Users model, int position) {
                final String user_id = getRef(position).getKey();

                viewHolder.setName(model.getName());
                viewHolder.setStatus(model.getStatus());
                viewHolder.setDp(model.getThumb_image(), getApplicationContext());

                viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        Intent profIntent = new Intent(AllUsersActivity.this, ProfileActivity.class);
                        profIntent.putExtra("from_user_id", user_id);
                        startActivity(profIntent);
                    }
                });
            }
        };

        allUsersRv.setAdapter(firebaseRecyclerAdapter);

    }

    public static class AllUsersViewHolder extends RecyclerView.ViewHolder{

        View mView;

        public AllUsersViewHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setStatus(String status){
            TextView user_status_tv = (TextView) mView.findViewById(R.id.user_item_status);
            user_status_tv.setText(status);
        }
        public void setName(String name){

            TextView user_name_tv = (TextView) mView.findViewById(R.id.user_item_name);
            user_name_tv.setText(name);
        }

        public void setDp(String thumb_image, Context ctx){
            CircleImageView userdp = (CircleImageView) mView.findViewById(R.id.user_item_dp);
            Picasso.with(ctx).load(thumb_image).placeholder(R.drawable.avatar).into(userdp);
        }
    }
}
