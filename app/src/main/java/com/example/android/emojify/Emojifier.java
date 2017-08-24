package com.example.android.emojify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import timber.log.Timber;

/**
 * Created by manvi on 23/8/17.
 */

public class Emojifier {

    private  enum Emoji{
        SMILE,
        FROWN,
        LEFT_WINK,
        RIGHT_WINK,
        LEFT_WINK_FROWN,
        RIGHT_WINK_FROWN,
        CLOSED_EYE_SMILE,
        CLOSED_EYE_FROWN
    }
    private static final float EMOJI_SCALE_FACTOR = .9f;
    private static final double SMILING_CONFIDENCE = .15;
    private static final double EYE_OPEN_CONFIDENCE = 0.5;

    public static Bitmap detectFacesAndOverlayEmoji(Context context, Bitmap bitmap){
        // Create the face detector, disable tracking and enable classifications
        FaceDetector detector = new FaceDetector.Builder(context)
                .setTrackingEnabled(false)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        //create Frame instance from the bitmap to supply to the detector:
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();

        //detector can be called synchronously with a frame to detect faces:
        SparseArray<Face> faces = detector.detect(frame);

        // Log the number of faces
        Timber.d("detectFaces: number of faces = " + faces.size());

        Bitmap resultBitmap = bitmap;
        if(faces.size()==0){
            Toast.makeText(context, R.string.no_faces_message, Toast.LENGTH_SHORT).show();
        }else {
            for (int i = 0; i < faces.size(); i++) {
                Bitmap emojiBitmap;
                Face face = faces.valueAt(i);
                switch (whichEmoji(face)) {
                    case SMILE:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.smile);
                        break;
                    case FROWN:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.frown);
                        break;
                    case LEFT_WINK:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.leftwink);
                        break;
                    case LEFT_WINK_FROWN:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.leftwinkfrown);
                        break;
                    case RIGHT_WINK:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.rightwink);
                        break;
                    case RIGHT_WINK_FROWN:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.rightwinkfrown);
                        break;
                    case CLOSED_EYE_SMILE:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.closed_smile);
                        break;
                    case CLOSED_EYE_FROWN:
                        emojiBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.closed_frown);
                        break;
                    default:
                        emojiBitmap = null;
                        Toast.makeText(context, R.string.no_emoji, Toast.LENGTH_SHORT).show();

                }
                resultBitmap = addBitmapToFace(resultBitmap,emojiBitmap,face);
            }
        }

        detector.release();
        return resultBitmap;
    }


    /**
     * Combines the original picture with the emoji bitmaps
     *
     * @param backgroundBitmap The original picture
     * @param emojiBitmap      The chosen emoji
     * @param face             The detected face
     * @return The final bitmap, including the emojis over the faces
     */
    private static Bitmap addBitmapToFace(Bitmap backgroundBitmap, Bitmap emojiBitmap, Face face) {

        // Initialize the results bitmap to be a mutable copy of the original image
        Bitmap resultBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(),
                backgroundBitmap.getHeight(), backgroundBitmap.getConfig());

        // Scale the emoji so it looks better on the face
        float scaleFactor = EMOJI_SCALE_FACTOR;

        // Determine the size of the emoji to match the width of the face and preserve aspect ratio
        int newEmojiWidth = (int) (face.getWidth() * scaleFactor);
        int newEmojiHeight = (int) (emojiBitmap.getHeight() *
                newEmojiWidth / emojiBitmap.getWidth() * scaleFactor);


        // Scale the emoji
        emojiBitmap = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false);

        // Determine the emoji position so it best lines up with the face
        float emojiPositionX =
                (face.getPosition().x + face.getWidth() / 2) - emojiBitmap.getWidth() / 2;
        float emojiPositionY =
                (face.getPosition().y + face.getHeight() / 2) - emojiBitmap.getHeight() / 3;

        // Create the canvas and draw the bitmaps to it
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null);

        return resultBitmap;
    }

    /**
     * Determines the closest emoji to the expression on the face, based on the
     * odds that the person is smiling and has each eye open.
     * @param face The face for which you pick an emoji..
     */

    private static Emoji whichEmoji(Face face){
        boolean isSmiling = false;
        boolean isLeftEyeClosed = false;
        boolean isRightEyeClosed= false;

        float leftEyeOpenConf = face.getIsLeftEyeOpenProbability();
        float rightEyeOpenConf = face.getIsRightEyeOpenProbability();
        float smilingConf = face.getIsSmilingProbability();

        Timber.d("whichEmoji: smilingProb = " + smilingConf);
        Timber.d("whichEmoji: leftEyeOpenProb = " + leftEyeOpenConf);
        Timber.d("whichEmoji: rightEyeOpenProb = " + rightEyeOpenConf);

        if(leftEyeOpenConf < EYE_OPEN_CONFIDENCE){
            isLeftEyeClosed = true;
        }
        if(rightEyeOpenConf < EYE_OPEN_CONFIDENCE){
            isRightEyeClosed = true;
        }
        if(smilingConf > SMILING_CONFIDENCE){
            isSmiling = true;
        }


        Emoji emoji;
        if(isSmiling){
            if(!isLeftEyeClosed && isRightEyeClosed){
                emoji = Emoji.RIGHT_WINK;
            }else if(isLeftEyeClosed && !isRightEyeClosed){
                emoji = Emoji.LEFT_WINK;
            } else if(isLeftEyeClosed){
                emoji = Emoji.CLOSED_EYE_SMILE;
            }else {
                emoji = Emoji.SMILE;
            }
        }else {
            if(!isLeftEyeClosed && isRightEyeClosed){
                emoji = Emoji.RIGHT_WINK_FROWN;
            }else if(isLeftEyeClosed && !isRightEyeClosed){
                emoji = Emoji.LEFT_WINK_FROWN;
            } else if(isLeftEyeClosed){
                emoji = Emoji.CLOSED_EYE_FROWN;
            }else {
                emoji = Emoji.FROWN;
            }
        }

        Timber.d("whichEmoji: " + emoji.name());
        return emoji;
    }
}
