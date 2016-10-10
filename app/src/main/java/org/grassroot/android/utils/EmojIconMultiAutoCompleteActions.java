package org.grassroot.android.utils;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.PopupWindow;

import hani.momanii.supernova_emoji_library.Helper.EmojiconGridView;
import hani.momanii.supernova_emoji_library.Helper.EmojiconMultiAutoCompleteTextView;
import hani.momanii.supernova_emoji_library.Helper.EmojiconsPopup;
import hani.momanii.supernova_emoji_library.emoji.Emojicon;


public class EmojIconMultiAutoCompleteActions{

    boolean useSystemEmoji=false;
    EmojiconsPopup popup;
    Context context;
    View rootView;
    ImageView emojiButton;
    EmojiconMultiAutoCompleteTextView emojiconMultiAutoCompleteTextView;
    int KeyBoardIcon= hani.momanii.supernova_emoji_library.R.drawable.ic_action_keyboard;
    int SmileyIcons= hani.momanii.supernova_emoji_library.R.drawable.smiley;
    hani.momanii.supernova_emoji_library.Actions.EmojIconActions.KeyboardListener keyboardListener;


    public EmojIconMultiAutoCompleteActions(Context ctx, View rootView, EmojiconMultiAutoCompleteTextView emojiconMultiAutoCompleteTextView, ImageView emojiButton)
    {
        this.emojiconMultiAutoCompleteTextView =emojiconMultiAutoCompleteTextView;
        this.emojiButton=emojiButton;
        this.context=ctx;
        this.rootView=rootView;
        this.popup = new EmojiconsPopup(rootView, ctx,useSystemEmoji);
    }



    public void setIconsIds(int keyboardIcon,int smileyIcon)
    {
        this.KeyBoardIcon=keyboardIcon;
        this.SmileyIcons=smileyIcon;
    }

    public void setUseSystemEmoji(boolean useSystemEmoji)
    {
        this.useSystemEmoji=useSystemEmoji;
        this.emojiconMultiAutoCompleteTextView.setUseSystemDefault(useSystemEmoji);
        refresh();
    }

    private void refresh()
    {
        popup.updateUseSystemDefault(useSystemEmoji);

    }


    public void ShowEmojIcon( )
    {

        //Will automatically set size according to the soft keyboard size
        popup.setSizeForSoftKeyboard();

        //If the emoji popup is dismissed, change emojiButton to smiley icon
        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {

            @Override
            public void onDismiss() {
                changeEmojiKeyboardIcon(emojiButton,SmileyIcons);
            }
        });

        //If the text keyboard closes, also dismiss the emoji popup
        popup.setOnSoftKeyboardOpenCloseListener(new EmojiconsPopup.OnSoftKeyboardOpenCloseListener() {

            @Override
            public void onKeyboardOpen(int keyBoardHeight) {
                if (keyboardListener != null)
                    keyboardListener.onKeyboardOpen();
            }

            @Override
            public void onKeyboardClose() {
                if (keyboardListener != null)
                    keyboardListener.onKeyboardClose();
                if(popup.isShowing())
                    popup.dismiss();
            }
        });

        popup.setOnEmojiconClickedListener(new EmojiconGridView.OnEmojiconClickedListener() {

            @Override
            public void onEmojiconClicked(Emojicon emojicon) {
                if (emojiconMultiAutoCompleteTextView == null || emojicon == null) {
                    return;
                }

                int start = emojiconMultiAutoCompleteTextView.getSelectionStart();
                int end = emojiconMultiAutoCompleteTextView.getSelectionEnd();
                if (start < 0) {
                    emojiconMultiAutoCompleteTextView.append(emojicon.getEmoji());
                } else {
                    emojiconMultiAutoCompleteTextView.getText().replace(Math.min(start, end),
                            Math.max(start, end), emojicon.getEmoji(), 0,
                            emojicon.getEmoji().length());
                }
            }
        });

        popup.setOnEmojiconBackspaceClickedListener(new EmojiconsPopup.OnEmojiconBackspaceClickedListener() {

            @Override
            public void onEmojiconBackspaceClicked(View v) {
                KeyEvent event = new KeyEvent(
                        0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
                emojiconMultiAutoCompleteTextView.dispatchKeyEvent(event);
            }
        });

        // To toggle between text keyboard and emoji keyboard keyboard(Popup)
        emojiButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //If popup is not showing => emoji keyboard is not visible, we need to show it
                if(!popup.isShowing()){

                    //If keyboard is visible, simply show the emoji popup
                    if(popup.isKeyBoardOpen()){
                        popup.showAtBottom();
                        changeEmojiKeyboardIcon(emojiButton,KeyBoardIcon);
                    }

                    //else, open the text keyboard first and immediately after that show the emoji popup
                    else{
                        emojiconMultiAutoCompleteTextView.setFocusableInTouchMode(true);
                        emojiconMultiAutoCompleteTextView.requestFocus();
                        popup.showAtBottomPending();
                        final InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(emojiconMultiAutoCompleteTextView, InputMethodManager.SHOW_IMPLICIT);
                        changeEmojiKeyboardIcon(emojiButton,KeyBoardIcon);
                    }
                }

                //If popup is showing, simply dismiss it to show the undelying text keyboard
                else{
                    popup.dismiss();
                }


            }
        });

    }


    public void closeEmojIcon()
    {
        if(popup!=null &&popup.isShowing())
            popup.dismiss();

    }

    private void changeEmojiKeyboardIcon(ImageView iconToBeChanged, int drawableResourceId){
        iconToBeChanged.setImageResource(drawableResourceId);
    }


    public interface KeyboardListener{
        void onKeyboardOpen();
        void onKeyboardClose();
    }

    public void setKeyboardListener(hani.momanii.supernova_emoji_library.Actions.EmojIconActions.KeyboardListener listener){
        this.keyboardListener = listener;
    }

}

