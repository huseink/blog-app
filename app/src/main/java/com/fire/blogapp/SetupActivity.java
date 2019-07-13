package com.fire.blogapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private CircleImageView setupImage;
    private Uri mainImageURI= null;
    private String user_id;
    private Boolean isChanged = false;

    private EditText setupName;
    private Button setupBtn;
    private ProgressBar setupProgress;

    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        Toolbar setupToolBar = findViewById(R.id.setup_toolbar);
        setSupportActionBar(setupToolBar);
        getSupportActionBar().setTitle("Account Setup");



        setupName= findViewById(R.id.setup_name);
        setupImage = findViewById(R.id.setup_image);
        setupBtn = findViewById(R.id.setup_btn);
        setupProgress = findViewById(R.id.setup_progress);


        setupProgress.setVisibility(View.VISIBLE);
        // User can not save changes until profile image and user_name are fetched.
        setupBtn.setEnabled(false);



        firebaseAuth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseFirestore = FirebaseFirestore.getInstance();
        user_id= firebaseAuth.getCurrentUser().getUid();


        //Gets current user's profile image and name from firestore
        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if(task.isSuccessful()){
                    if(task.getResult().exists()){
                        String name = task.getResult().getString("name");
                        String image = task.getResult().getString("image");

                        mainImageURI=Uri.parse(image);

                        setupName.setText(name);

                        RequestOptions placeholdeRequest = new RequestOptions();
                        placeholdeRequest.placeholder(R.drawable.defaultprofile);
                        Glide.with(SetupActivity.this).load(image).into(setupImage);


                    }
                }else{
                    Toast.makeText(SetupActivity.this, "Data Retrieve Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }

                setupProgress.setVisibility(View.INVISIBLE);
            }
        });


        setupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String user_name = setupName.getText().toString();

                //If the profile image is changed
                if(isChanged){
                    //Store the image in storage
                    if(!TextUtils.isEmpty(user_name) && mainImageURI !=null){

                        user_id = firebaseAuth.getCurrentUser().getUid();
                        setupProgress.setVisibility(View.VISIBLE);

                        final StorageReference image_path = storageReference.child("profile_images").child(user_id +".jpg");

                        image_path.putFile(mainImageURI).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }
                                return image_path.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                // If stored succesfully
                                if (task.isSuccessful()) {
                                    // Proceed to store username and imageUri in FireStore (DATABASE)
                                    storeFirestore(task, user_name);

                                } else {
                                    setupProgress.setVisibility(View.INVISIBLE);
                                    Toast.makeText(SetupActivity.this, "Image Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }

                            }

                        });

                    }


                }else{
                    // If the profile image is not changed , change the username only
                    storeFirestore(null, user_name);
                }

            }
        });


        setupImage = findViewById(R.id.setup_image);


        setupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // If user is running Marhsmallow or greater version then the permission is required from the user
                //  Check if READ_EXTERNAL_STORAGE permission is given.
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // If the permission is not granted then request permission.
                    if (ContextCompat.checkSelfPermission(SetupActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(SetupActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                    }else{
                        //If the permission is granted then open up Crop Activity.
                        imageCropper();
                    }
                }else{
                    //If user is running older Android versions, app is not required to get permission from the user.
                    imageCropper();
                }

            }
        });
    }

    private void storeFirestore(Task<Uri> task ,String user_name) {
        Uri downloadUri;

        //If image is changed
        if(task != null) {

            downloadUri = task.getResult();

        } else {
            //If image is not changed
            downloadUri = mainImageURI;

        }

        // Store user_name and profile image URI as a Hash Map
        Map<String , String> userMap = new HashMap<>();
        userMap.put("name",user_name);
        userMap.put("image",downloadUri.toString());

        //Create a collection with userMap
        firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
            // If stored sucessfully
                if(task.isSuccessful()){
                    //Send user to MainActivity
                    Intent mainIntent = new Intent(SetupActivity.this,MainActivity.class);
                    startActivity(mainIntent);
                    finish();

                }else{
                    Toast.makeText(SetupActivity.this, "FireStore Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
                setupProgress.setVisibility(View.INVISIBLE);
            }
        });
    }

    // If Crop image activity returns RESULT_OK
    // Set the profile image as cropped image
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mainImageURI = result.getUri();
                setupImage.setImageURI(mainImageURI);
                isChanged= true;
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }


    // Opens up Image Cropper Activity
    private void imageCropper(){
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1,1)
                .start(SetupActivity.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is accepted, the result arrays lenghts are greater than 0.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {


                    // What to do when the request is given.
                    imageCropper();
                }
                // If request is cancelled, the result arrays are empty.
                else {
                    Toast.makeText(SetupActivity.this, "Please allow required permission(s)", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' statements for other permssions
        }
    }

}
