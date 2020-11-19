package com.example.ardesigner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ArFragment arFrag;

    private Button clear_button;
    private Button clear_save_button;
    private Button load_button;

    private ImageButton chair_button;
    private ImageButton couch_button;
    private ImageButton piano_button;
    private ImageButton table_button;
    private ImageButton windturbine_button;
    private String selected = "chair.sfb";

    private enum AnchorState {
        NONE, HOSTING, HOSTED
    }
    private AnchorState anchorState = AnchorState.NONE;
    private int num_anchor_object = 0;
    private Anchor anchor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set up object selection
        chair_button = findViewById(R.id.chair);
        chair_button.setOnClickListener((view) -> {
            selected = "chair.sfb";
        });
        couch_button = findViewById(R.id.couch);
        couch_button.setOnClickListener((view) -> {
            selected = "couch.sfb";
        });
        piano_button = findViewById(R.id.piano);
        piano_button.setOnClickListener((view) -> {
            selected = "piano.sfb";
        });
        table_button = findViewById(R.id.table);
        table_button.setOnClickListener((view) -> {
            selected = "Table_Small_Circular_01.sfb";
        });
        windturbine_button = findViewById(R.id.windTurbine);
        windturbine_button.setOnClickListener((view) -> {
            selected = "windturbine.sfb";
        });

        // set up an AR fragment
        arFrag = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragment);

        // get the location of surface (store in hitResult)
        arFrag.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            // create an anchor for that location
            anchor = hitResult.createAnchor();

            anchor = arFrag.getArSceneView().getSession().hostCloudAnchor(anchor);
            anchorState = AnchorState.HOSTING;
            num_anchor_object++;
            //Toast.makeText(this, "Hosting in progress.", Toast.LENGTH_LONG).show();

            // using the anchor location, place model on the anchored location
            ModelRenderable.builder()
                    .setSource(this, Uri.parse(selected))
                    .build()
                    .thenAccept(modelRenderable -> addModelToScene(anchor, modelRenderable));
        });
        SharedPreferences sharedPreferences = getSharedPreferences("savedObject", Context.MODE_PRIVATE);
        arFrag.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if (anchorState != AnchorState.HOSTING) {
                return;
            }
            Anchor.CloudAnchorState cloudAnchorState = anchor.getCloudAnchorState();
            if (cloudAnchorState.isError()) {
                Toast.makeText(this, "Object " + num_anchor_object + " fail to save.", Toast.LENGTH_LONG).show();
            } else if (cloudAnchorState == Anchor.CloudAnchorState.SUCCESS) {
                anchorState = AnchorState.HOSTED;
                // get anchor id and save it with shared preference
                String anchorID = anchor.getCloudAnchorId();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("anchorID" + num_anchor_object, anchorID);
                editor.putString("anchor_ID_object" + num_anchor_object, selected);
                editor.putInt("num_anchor_object", num_anchor_object);
                editor.apply();
                Toast.makeText(this, "Object " + num_anchor_object + " saved successfully.", Toast.LENGTH_LONG).show();
            }
        });

        clear_button = (Button) findViewById(R.id.clear_button);
        clear_button.setOnClickListener((view) -> {
            List<Node> nodeList = new ArrayList<>(arFrag.getArSceneView().getScene().getChildren());
            for (Node node : nodeList) {
                // check if a node is an anchor node
                if ( node instanceof AnchorNode) {
                    // remove anchor
                    if (((AnchorNode) node).getAnchor() != null) {
                        ((AnchorNode) node).getAnchor().detach();
                        node.setParent(null);
                    }
                }
            }
            num_anchor_object = 0;
        });

        clear_save_button = (Button) findViewById(R.id.clear_save_button);
        clear_save_button.setOnClickListener((view) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.commit();
            Toast.makeText(this, "Save Data Cleared Successfully.", Toast.LENGTH_LONG).show();
        });


        load_button = (Button) findViewById(R.id.load_button);
        load_button.setOnClickListener((view) -> {
            num_anchor_object = sharedPreferences.getInt("num_anchor_object", 0);
            if (num_anchor_object == 0) {
                Toast.makeText(this, "No object saved", Toast.LENGTH_LONG).show();
            }
            for (int i = 1; i <= num_anchor_object; i++) {
                String anchorID = sharedPreferences.getString("object" + i, "");
                String anchor_ID_object = sharedPreferences.getString("anchor_ID_object" + i, "");
                Anchor savedAnchor = arFrag.getArSceneView().getSession().resolveCloudAnchor(anchorID);
                ModelRenderable.builder()
                        .setSource(this, Uri.parse(anchor_ID_object))
                        .build()
                        .thenAccept(modelRenderable -> addModelToScene(savedAnchor, modelRenderable));
            }
            Toast.makeText(this, "Load Completed", Toast.LENGTH_LONG).show();
        });


    }

    private void addModelToScene(Anchor anchor, ModelRenderable modelRenderable) {
        // make an anchor node from anchor location
        AnchorNode anchorNode = new AnchorNode(anchor);

        // make the anchorNode a transformable node such that it can be resize
        TransformableNode transNode = new TransformableNode(arFrag.getTransformationSystem());
        transNode.setParent(anchorNode);
        transNode.setRenderable(modelRenderable);
        arFrag.getArSceneView().getScene().addChild(anchorNode);
        transNode.select();
    }
}
