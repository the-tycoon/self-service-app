package org.mifos.selfserviceapp.ui.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.mifos.selfserviceapp.R;
import org.mifos.selfserviceapp.api.local.PreferencesHelper;
import org.mifos.selfserviceapp.models.accounts.savings.SavingsWithAssociations;
import org.mifos.selfserviceapp.models.accounts.savings.Status;
import org.mifos.selfserviceapp.presenters.SavingAccountsDetailPresenter;
import org.mifos.selfserviceapp.ui.activities.base.BaseActivity;
import org.mifos.selfserviceapp.ui.enums.AccountType;
import org.mifos.selfserviceapp.ui.enums.ChargeType;
import org.mifos.selfserviceapp.ui.fragments.base.BaseFragment;
import org.mifos.selfserviceapp.ui.views.SavingAccountsDetailView;
import org.mifos.selfserviceapp.utils.CircularImageView;
import org.mifos.selfserviceapp.utils.Constants;
import org.mifos.selfserviceapp.utils.CurrencyUtil;
import org.mifos.selfserviceapp.utils.DateHelper;
import org.mifos.selfserviceapp.utils.QrCodeGenerator;
import org.mifos.selfserviceapp.utils.SymbolsUtils;
import org.mifos.selfserviceapp.utils.Toaster;
import org.mifos.selfserviceapp.utils.Utils;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @author Vishwajeet
 * @since 18/8/16.
 */

public class SavingAccountsDetailFragment extends BaseFragment implements SavingAccountsDetailView {

    @BindView(R.id.tv_account_status)
    TextView tvAccountStatus;

    @BindView(R.id.iv_circle_status)
    CircularImageView ivCircularStatus;

    @BindView(R.id.tv_total_withdrawals)
    TextView tvTotalWithDrawals;

    @BindView(R.id.tv_min_req_bal)
    TextView tvMiniRequiredBalance;

    @BindView(R.id.tv_saving_account_number)
    TextView tvSavingAccountNumber;

    @BindView(R.id.tv_nominal_interest_rate)
    TextView tvNominalInterestRate;

    @BindView(R.id.tv_total_deposits)
    TextView tvTotalDeposits;

    @BindView(R.id.tv_acc_balance)
    TextView tvAccountBalanceMain;

    @BindView(R.id.tv_last_transaction)
    TextView tvLastTransaction;

    @BindView(R.id.made_on)
    TextView tvMadeOnTextView;

    @BindView(R.id.tv_made_on)
    TextView tvMadeOnTransaction;

    @BindView(R.id.ll_account)
    LinearLayout layoutAccount;

    @BindView(R.id.ll_error)
    View layoutError;

    @BindView(R.id.tv_status)
    TextView tvStatus;

    @Inject
    PreferencesHelper preferencesHelper;

    @Inject
    SavingAccountsDetailPresenter mSavingAccountsDetailPresenter;

    private View rootView;
    private long savingsId;
    private Status status;
    private SavingsWithAssociations savingsWithAssociations;

    public static SavingAccountsDetailFragment newInstance(long savingsId) {
        SavingAccountsDetailFragment fragment = new SavingAccountsDetailFragment();
        Bundle args = new Bundle();
        args.putLong(Constants.SAVINGS_ID, savingsId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            savingsId = getArguments().getLong(Constants.SAVINGS_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_saving_account_details, container, false);
        ((BaseActivity) getActivity()).getActivityComponent().inject(this);
        setToolbarTitle(getString(R.string.saving_account_details));
        ButterKnife.bind(this, rootView);
        mSavingAccountsDetailPresenter.attachView(this);

        mSavingAccountsDetailPresenter.loadSavingsWithAssociations(savingsId);

        return rootView;
    }

    /**
     * Opens up Phone Dialer
     */
    @OnClick(R.id.tv_help_line_number)
    void dialHelpLineNumber() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + getString(R.string.help_line_number)));
        startActivity(intent);
    }

    /**
     * Opens {@link SavingsMakeTransferFragment} if status is ACTIVE else shows a
     * {@link android.support.design.widget.Snackbar} that Account should be Active
     */
    @OnClick(R.id.tv_deposit)
    void deposit() {
        if (status.getActive()) {
            ((BaseActivity) getActivity()).replaceFragment(SavingsMakeTransferFragment
                    .newInstance(savingsId, Constants.TRANSFER_PAY_TO), true, R.id.container);
        } else {
            Toaster.show(rootView, getString(R.string.account_not_active_to_perform_deposit));
        }
    }

    /**
     * Opens {@link SavingsMakeTransferFragment} if status is ACTIVE else shows a
     * {@link android.support.design.widget.Snackbar} that Account should be Active
     */
    @OnClick(R.id.tv_make_a_transfer)
    void transfer() {
        if (status.getActive()) {
            ((BaseActivity) getActivity()).replaceFragment(SavingsMakeTransferFragment
                    .newInstance(savingsId, Constants.TRANSFER_PAY_FROM), true, R.id.container);
        } else {
            Toaster.show(rootView, getString(R.string.account_not_active_to_perform_transfer));
        }
    }

    /**
     * Sets Saving account basic info fetched from the server
     * @param savingsWithAssociations object containing details of a saving account
     */
    @Override
    public void showSavingAccountsDetail(SavingsWithAssociations savingsWithAssociations) {
        layoutAccount.setVisibility(View.VISIBLE);

        String currencySymbol = savingsWithAssociations.getCurrency().getDisplaySymbol();
        Double accountBalance = savingsWithAssociations.getSummary().getAccountBalance();

        tvAccountStatus.setText(savingsWithAssociations.getClientName());
        if (savingsWithAssociations.getMinRequiredOpeningBalance() != null) {
            tvMiniRequiredBalance.setText(getString(R.string.string_and_string, currencySymbol,
                    CurrencyUtil.formatCurrency(getActivity(), savingsWithAssociations.
                    getMinRequiredOpeningBalance())));
        }
        tvTotalWithDrawals.setText(getString(R.string.string_and_string,
                currencySymbol, CurrencyUtil.formatCurrency(getActivity(), savingsWithAssociations.
                        getSummary().getTotalWithdrawals())));
        tvAccountBalanceMain.setText(getString(R.string.string_and_string,
                currencySymbol, CurrencyUtil.formatCurrency(getActivity(), accountBalance)));
        tvNominalInterestRate.setText(getString(R.string.double_and_String,
                savingsWithAssociations.getNominalAnnualInterestRate(), SymbolsUtils.PERCENT));
        tvSavingAccountNumber.setText(String.valueOf(savingsWithAssociations.getAccountNo()));
        tvTotalDeposits.setText(getString(R.string.double_and_String,
                savingsWithAssociations.getSummary().getTotalDeposits(), currencySymbol));

        if (!savingsWithAssociations.getTransactions().isEmpty()) {
            tvLastTransaction.setText(getString(R.string.double_and_String,
                    savingsWithAssociations.getTransactions().get(0).getAmount(), currencySymbol));
            tvMadeOnTransaction.setText(DateHelper.getDateAsString(
                    savingsWithAssociations.getLastActiveTransactionDate()));
        } else {
            tvLastTransaction.setText(R.string.no_transaction);
            tvMadeOnTransaction.setVisibility(View.GONE);
            tvMadeOnTextView.setVisibility(View.GONE);
        }

        this.savingsWithAssociations = savingsWithAssociations;
        showAccountStatus(savingsWithAssociations);
    }

    /**
     * It is called whenever any error occurs while executing a request
     * @param message Error message that tells the user about the problem.
     */
    @Override
    public void showErrorFetchingSavingAccountsDetail(String message) {
        layoutAccount.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        tvStatus.setText(message);
    }

    /**
     * Sets the status of account i.e. {@code tvAccountStatus} and {@code ivCircularStatus} color
     * according to {@code savingsWithAssociations}
     * @param savingsWithAssociations object containing details of a saving account
     */
    @Override
    public void showAccountStatus(SavingsWithAssociations savingsWithAssociations) {
        status = savingsWithAssociations.getStatus();
        if (status.getActive()) {
            ivCircularStatus.setImageDrawable(
                    Utils.setCircularBackground(R.color.deposit_green, getActivity()));
            tvAccountStatus.setText(R.string.active);
        } else if (status.getApproved()) {
            ivCircularStatus.setImageDrawable(
                    Utils.setCircularBackground(R.color.blue, getActivity()));
            tvAccountStatus.setText(R.string.need_approval);
        } else if (status.getSubmittedAndPendingApproval()) {
            ivCircularStatus.setImageDrawable(
                    Utils.setCircularBackground(R.color.light_yellow, getActivity()));
            tvAccountStatus.setText(R.string.pending);
        } else if (status.getMatured()) {
            ivCircularStatus.setImageDrawable(
                    Utils.setCircularBackground(R.color.red_light, getActivity()));
            tvAccountStatus.setText(R.string.matured);
        } else {
            ivCircularStatus.setImageDrawable(
                    Utils.setCircularBackground(R.color.black, getActivity()));
            tvAccountStatus.setText(R.string.closed);
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void showProgress() {
        layoutAccount.setVisibility(View.GONE);
        showProgressBar();
    }

    @Override
    public void hideProgress() {
        layoutAccount.setVisibility(View.VISIBLE);
        hideProgressBar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        hideProgressBar();
        mSavingAccountsDetailPresenter.detachView();
    }

    @OnClick(R.id.ll_savings_transactions)
    public void transactionsClicked() {
        ((BaseActivity) getActivity()).replaceFragment(SavingAccountsTransactionFragment.
                newInstance(savingsId), true, R.id.container);
    }

    @OnClick(R.id.ll_savings_charges)
    public void chargeClicked() {
        ((BaseActivity) getActivity()).replaceFragment(ClientChargeFragment.
                newInstance(savingsId, ChargeType.SAVINGS), true, R.id.container);
    }

    @OnClick(R.id.ll_savings_qr_code)
    public void qrCodeClicked() {
        String accountDetailsInJson = QrCodeGenerator.getAccountDetailsInString(
                savingsWithAssociations.getAccountNo(), preferencesHelper.getOfficeName(),
                AccountType.SAVINGS);
        ((BaseActivity) getActivity()).replaceFragment(QrCodeDisplayFragment.
                newInstance(accountDetailsInJson), true, R.id.container);
    }

}
