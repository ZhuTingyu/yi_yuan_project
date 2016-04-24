package com.yuan.skeleton.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.yuan.skeleton.R;
import com.yuan.skeleton.application.Injector;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by KevinLee on 2016/4/24.
 */
public class UserProposalFragment extends WebViewBaseFragment {

    @InjectView(R.id.proposal)
    Button proposal;
    @InjectView(R.id.complaint)
    Button complaint;

    public static UserProposalFragment newInstance() {
        UserProposalFragment fragment = new UserProposalFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = createView(inflater, R.layout.fragment_proposal_user, container, savedInstanceState);

        Injector.inject(this);

        ButterKnife.reset(this);
        ButterKnife.inject(this, view);

        redirectToLoadUrl("user_center.html");
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        proposal.performClick();
    }

    @OnClick({R.id.proposal,R.id.complaint})
    public void onClick(View view){
        switch (view.getId()){
            case R.id.proposal:
                proposal.setEnabled(false);
                complaint.setEnabled(true);
                break;
            case R.id.complaint:
                proposal.setEnabled(true);
                complaint.setEnabled(false);
                break;
        }
    }

}
