package com.example.lilactests.addeditnote;

import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.example.lilactests.R;
import com.example.lilactests.listener.OnDatePickListener;
import com.example.lilactests.listener.OnTimePickListener;
import com.example.lilactests.model.domain.Note;
import com.example.lilactests.receiver.ReminderReceiver;
import com.example.lilactests.utils.TimeUtils;
import com.example.lilactests.utils.ToastUtils;
import com.example.lilactests.view.dialogfragment.DatePickerFragment;
import com.example.lilactests.view.dialogfragment.TimePickerFragment;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Fragment Has Note Content ,use in AddNoteActivity and EditNoteActivity.
 */
public class EditNoteFragment extends Fragment {
    private static final String TAG = "EditNoteActivity";
    private static final String TIME_PICK_DIALOG = "TimePickerDialog";
    private static final String DATE_PICK_DIALOG = "DatePickerDialog";
    @BindView(R.id.et_title)
    EditText mEtTitle;
    @BindView(R.id.tv_date)
    TextView mTvDate;
    @BindView(R.id.tv_time)
    TextView mTvTime;
    @BindView(R.id.cb_alarm)
    CheckBox mCbAlarm;
    @BindView(R.id.et_content)
    EditText mEtContent;
    private boolean isPicked = false;
    private PendingIntent alarmIntent;
    private AlarmManager alarmMgr;
    private Intent mIntent;
    private View mFragmentView;

    public void setItemID(Long itemID) {
        this.mItemID = itemID;
    }

    private long mItemID = -1L;

    public void setPicked(boolean picked) {
        isPicked = picked;
    }

    public EditNoteFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mFragmentView = inflater.inflate(R.layout.fragment_edit_note, container, false);
        ButterKnife.bind(this, mFragmentView);
        alarmMgr = (AlarmManager) getActivity().
                getSystemService(Context.ALARM_SERVICE);
        initView();
        return mFragmentView;
    }

    private void initView() {
        toggleSoftKeyboard();

        mCbAlarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //first time, so pick date and time.
                    if (!isPicked) {
                        pickDate();
                    }
                    //has date and time ,just show it.
                    showDateTimeViews();
                } else {
                    hideDateTimeViews();
                }
            }
        });
    }

    private void hideDateTimeViews() {
        mTvDate.setVisibility(View.INVISIBLE);
        mTvTime.setVisibility(View.INVISIBLE);
        // If the alarm has been set, cancel it.
        cancelReminder();
    }

    private void cancelReminder() {
        if (alarmIntent == null && mItemID != -1L) {
            //not add note
            mIntent = new Intent(getActivity(), ReminderReceiver.class);
            alarmIntent = PendingIntent.getBroadcast(getActivity(), (int) mItemID, mIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
        alarmMgr.cancel(alarmIntent);
    }

    private void showDateTimeViews() {
        mTvDate.setVisibility(View.VISIBLE);
        mTvTime.setVisibility(View.VISIBLE);
    }

    private void toggleSoftKeyboard() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                InputMethodManager inputManager =
                        (InputMethodManager) mEtTitle.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(mEtTitle, 0);
            }
        }, 500);
    }

    public Note getNote(Boolean isAddNoted, long id) {
        Note note = new Note();
        note.title = String.valueOf(mEtTitle.getText());
        note.content = String.valueOf(mEtContent.getText());
        note.hasAlarm = mCbAlarm.isChecked();
        if (isAddNoted) {
            note.createDate = new Date();
            note.id = System.currentTimeMillis();
        } else {
            note.id = id;
        }
        note.date = new Date();

        String date = String.valueOf(mTvDate.getText()) + " " +
                String.valueOf(mTvTime.getText());
        if (!TextUtils.isEmpty(date.trim())) {
            note.date = TimeUtils.parseText(date);
        }

        if (note.hasAlarm) {
            addReminder(note.date, note);
        }

        return note;
    }

    private void PickTimeIfCreateAlarm() {
        if (!isPicked) {
            pickTime();
        }
    }

    private void pickDate() {
        DatePickerFragment datePickerDialog = new DatePickerFragment();
        datePickerDialog.setOnDatePickListener(new OnDatePickListener() {
            @Override
            public void onDatePick(int year, int monthOfYear, int dayOfMonth) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, monthOfYear, dayOfMonth);
                Date date = new Date(calendar.getTimeInMillis());
                String text = TimeUtils.formatDate(date);
                mTvDate.setText(text);
                confirmTimeValidity(year, monthOfYear, dayOfMonth);
                PickTimeIfCreateAlarm();
            }

            @Override
            public void onDatePickCancel() {
                if (!isPicked) {
                    mCbAlarm.setChecked(false);
                }
            }
        });
        datePickerDialog.show(getFragmentManager(), DATE_PICK_DIALOG);
    }

    private void confirmTimeValidity(int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, monthOfYear, dayOfMonth);
        if (!TimeUtils.isSameDay(calendar, Calendar.getInstance())) {
            return;
        }
        String timeStr = mTvTime.getText().toString().trim();
        if (TextUtils.isEmpty(timeStr)) {
            return;
        }
        Date timeDate = TimeUtils.parseTime(timeStr);
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(timeDate);

        rightNow.set(Calendar.YEAR, year);
        rightNow.set(Calendar.MONTH, monthOfYear);
        rightNow.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        if (rightNow.getTimeInMillis() < System.currentTimeMillis() + 60000) {
            //Alarm time over now less than one minute,choose time again
            ToastUtils.showToast(getActivity(), "The time has passed");
            pickDate();
        }
    }

    public boolean confirmNoteComplete() {
        if (TextUtils.isEmpty(mEtTitle.getText())) {
            ToastUtils.showToast(getActivity(), "title can't be empty");
            return false;
        }
        return true;
    }

    private void pickTime() {
        TimePickerFragment timePickDialog = new TimePickerFragment();
        timePickDialog.setOnTimePickListener(new OnTimePickListener() {
            @Override
            public void onTimePick(int hourOfDay, int minute) {
                String dateStr = String.valueOf(mTvDate.getText());
                Date date = TimeUtils.parseDate(dateStr);
                Calendar tvCalendar = Calendar.getInstance();
                tvCalendar.setTime(date);

                boolean isToday = TimeUtils.isSameDay(tvCalendar, Calendar.getInstance());
                //if the date is today,decide time has not passed
                Calendar rightNow = Calendar.getInstance();
                rightNow.set(Calendar.HOUR_OF_DAY, hourOfDay);
                rightNow.set(Calendar.MINUTE, minute);
                if (isToday) {
                    confirmTimePast(rightNow);
                }
                String time = TimeUtils.formatTime(rightNow.getTime());
                mTvTime.setText(time);
                if (!isPicked) {
                    isPicked = true;
                }
            }

            private void confirmTimePast(Calendar rightNow) {
                if (rightNow.getTimeInMillis() < System.currentTimeMillis() + 60000) {
                    //Alarm time over now less than one minute,choose time again
                    ToastUtils.showToast(getActivity(), "The time has passed");
                    pickTime();
                }
            }

            @Override
            public void onTimePickCancel() {
                if (!isPicked) {
                    mCbAlarm.setChecked(false);
                }
            }
        });
        timePickDialog.show(getFragmentManager(), TIME_PICK_DIALOG);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @OnClick({R.id.tv_date, R.id.tv_time})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_date:
                pickDate();
                break;
            case R.id.tv_time:
                pickTime();
                break;
        }
    }

    private void addReminder(Date date, Note item) {
        mIntent = new Intent(getActivity(), ReminderReceiver.class);
        mIntent.putExtra("title", item.title);
        mIntent.putExtra("content", item.content);

        alarmIntent = PendingIntent.getBroadcast(getActivity(), (int) item.id, mIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, date.getTime(), alarmIntent);
    }

    public void setTitleText(String title) {
        mEtTitle.setText(title);
    }

    public void setContentText(String content) {
        mEtContent.setText(content);
    }

    public void setAlarmChecked(boolean isChecked) {
        mCbAlarm.setChecked(isChecked);
    }

    public void setDateText(String date) {
        mTvDate.setText(date);
    }

    public void setTimeText(String time) {
        mTvTime.setText(time);
    }

    public boolean isNoteEmpty() {
        return TextUtils.isEmpty(mEtTitle.getText())
                && TextUtils.isEmpty(mEtContent.getText())
                && !mCbAlarm.isChecked();
    }
}
