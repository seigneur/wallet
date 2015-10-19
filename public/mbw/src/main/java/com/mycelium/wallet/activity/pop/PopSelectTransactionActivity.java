/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet.activity.pop;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.main.TransactionArrayAdapter;
import com.mycelium.wallet.pop.PopRequest;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.WalletAccount;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PopSelectTransactionActivity extends FragmentActivity {
    private PopRequest popRequest;
    private MbwManager mbwManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pop_select_transaction_activity);

        PopRequest popRequest = (PopRequest) getIntent().getSerializableExtra("popRequest");
        if (popRequest == null) {
            finish();
        }
        this.popRequest = popRequest;


        mbwManager = MbwManager.getInstance(getApplicationContext());
        WalletAccount account = mbwManager.getSelectedAccount();
        if (account.isArchived()) {
            return;
        }
        HistoryPagerAdapter pagerAdapter = new HistoryPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(pagerAdapter);

    }

    public class HistoryPagerAdapter extends FragmentPagerAdapter {

        private ListFragment matchingTransactionsFragment = null;
        private ListFragment nonMatchingTransactionsFragment = null;

        public HistoryPagerAdapter(FragmentManager fm) {
            super(fm);

            WalletAccount account = mbwManager.getSelectedAccount();

            List<TransactionSummary> history = account.getTransactionHistory(0, 1000);
            List<TransactionSummary> matchingTransactions = new ArrayList<TransactionSummary>();
            List<TransactionSummary> nonMatchingTransactions = new ArrayList<TransactionSummary>();

            for (TransactionSummary transactionSummary : history) {
                if (transactionSummary.value >= 0L) {
                    // We are only interested in payments
                    continue;
                }
                if (PopUtils.matches(popRequest, mbwManager.getMetadataStorage(), transactionSummary)) {
                    matchingTransactions.add(transactionSummary);
                } else {
                    nonMatchingTransactions.add(transactionSummary);
                }
            }

            Map<Address, String> addressBook = mbwManager.getMetadataStorage().getAllAddressLabels();

            matchingTransactionsFragment = new ListFragment() {
                @Override
                public void onViewCreated(View view, Bundle savedInstanceState) {
                    super.onViewCreated(view, savedInstanceState);
                    setEmptyText(getText(R.string.pop_no_matching_transactions));
                }
            };
            matchingTransactionsFragment.setListAdapter(new TransactionHistoryAdapter(PopSelectTransactionActivity.this, matchingTransactions, addressBook));

            nonMatchingTransactionsFragment = new ListFragment();
            nonMatchingTransactionsFragment.setListAdapter(new TransactionHistoryAdapter(PopSelectTransactionActivity.this, nonMatchingTransactions, addressBook));
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return matchingTransactionsFragment;
                case 1:
                    return nonMatchingTransactionsFragment;
                default:
                    throw new RuntimeException("Unknown fragemt id " + i);
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(position == 0 ? R.string.pop_matching_transactions : R.string.pop_non_matching_transactions);
        }
    }

    private class TransactionHistoryAdapter extends TransactionArrayAdapter {

        public TransactionHistoryAdapter(Context context, List<TransactionSummary> objects, Map<Address, String> addressBook) {
            super(context, objects, addressBook);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent signPopIntent = new Intent(PopSelectTransactionActivity.this, PopActivity.class);
                    signPopIntent.putExtra("popRequest", popRequest);
                    signPopIntent.putExtra("selectedTransactionToProve", getItem(position).txid);
                    startActivity(signPopIntent);
                    finish();
                }
            });
            return view;
        }
    }

}
