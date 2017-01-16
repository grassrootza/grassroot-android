package org.grassroot.android.fragments;

import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import org.grassroot.android.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.Unbinder;
import rx.functions.Action1;

/**
 * Created by luke on 2017/01/13.
 */

public class SingleInputFragment extends Fragment {

    private static final String HEADER = "header";
    private static final String EXPLAN = "explan";
    private static final String HINT = "hint";
    private static final String NEXT = "next";

    private Action1<String> subscriber;
    private Unbinder unbinder;

    @BindView(R.id.header) TextView header;
    @BindView(R.id.explanation) TextView explanation;
    @BindView(R.id.text_input) TextInputEditText input;
    @BindView(R.id.next) Button next;

    public static class SingleInputBuilder {

        private int headerResource;
        private int explanResource;
        private int inputHint;
        private int nextText;

        private Action1<String> subscriber;

        public SingleInputBuilder header(int headerResource) {
            this.headerResource = headerResource;
            return this;
        }

        public SingleInputBuilder explanation(int explanResource) {
            this.explanResource = explanResource;
            return this;
        }

        public SingleInputBuilder hint(int hintResource) {
            this.inputHint = hintResource;
            return this;
        }

        public SingleInputBuilder next(int nextResource) {
            this.nextText = nextResource;
            return this;
        }

        public SingleInputBuilder subscriber(Action1<String> subscriber) {
            this.subscriber = subscriber;
            return this;
        }

        public SingleInputFragment build() {
            SingleInputFragment fragment = new SingleInputFragment();
            fragment.subscriber = subscriber;
            Bundle args = new Bundle();
            args.putInt(HEADER, headerResource);
            args.putInt(EXPLAN, explanResource);
            args.putInt(HINT, inputHint);
            args.putInt(NEXT, nextText);
            fragment.setArguments(args);
            return fragment;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_single_text_input, container, false);
        unbinder = ButterKnife.bind(this, view);

        Bundle args = getArguments();
        header.setText(args.getInt(HEADER));
        explanation.setText(args.getInt(EXPLAN));
        input.setHint(args.getInt(HINT));
        next.setText(args.getInt(NEXT));

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.next)
    public void onClickNext() {
        passToSubscriber();
    }

    @OnEditorAction(R.id.text_input)
    public boolean onTextNext(int actionId, KeyEvent keyEvent) {
        if (actionId == EditorInfo.IME_ACTION_NEXT) {
            passToSubscriber();
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void passToSubscriber() {
        subscriber.call(input.getText().toString().trim());
    }

    protected void displayError(String errorText) {
        input.setError(errorText);
        input.requestFocus();
    }

}
