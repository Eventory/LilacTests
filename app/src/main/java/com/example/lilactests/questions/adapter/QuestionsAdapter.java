package com.example.lilactests.questions.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.lilactests.R;
import com.example.lilactests.config.MultiSelector;
import com.example.lilactests.model.domain.Question;
import com.example.lilactests.questiondetail.QuestionDetailActivity;
import com.example.lilactests.questions.QuestionsPresenter;
import com.example.lilactests.utils.RxBus;
import com.example.lilactests.utils.TimeUtils;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;

/**
 * Created by wing on 2016/4/19.
 * Adapter For List Questions,support choice mode.
 */
public class QuestionsAdapter extends RecyclerView.Adapter<QuestionsAdapter.QuestionViewHolder> {
    private static final String TAG = "QuestionsAdapter";
    private Context mContext;
    private List<Question> mQuestionsList;
    private MultiSelector mSelector;  //多选
    private boolean isMultiMode;      //开启多选模式
    private ActionMode.Callback mActionSelectMode = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {//临时占据了ActionBar的位置
            new MenuInflater(mContext).inflate(R.menu.menu_multi_mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            setSelectable(true);
            isMultiMode = true;
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    deleteSelectedQuestions();
                    break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            setSelectable(false);
            isMultiMode = false;
            mSelector.clear();
            notifyDataSetChanged();
        }
    };

    private void deleteSelectedQuestions() { //删除条目功能 并且加入了确认功能

        new AlertDialog.Builder(mContext)
                .setTitle(R.string.title_delete)
                .setMessage(R.string.content_delete)
                .setNegativeButton(R.string.btn_cancel, null) // negative button 本身就有关闭的作用 所以传入参数null 表示没有额外的动作
                .setPositiveButton(R.string.btn_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {//点击之后 删除选中的条目 remove mQuestionsList和 mSelector
                        for (Question item : mQuestionsList) {
                            if (mSelector.contains(item.ID)) {
                                mQuestionsList.remove(item);
                                mSelector.remove(item.ID);
                                new QuestionsPresenter(mContext, null).deleteQuestion(item.ID);//再新创建一个 QuestionsPresenter MVP架构中的业务逻辑部分
                            }
                        }
                        notifyDataSetChanged(); //有时候我们需要修改已经生成的列表，添加或者修改数据，notifyDataSetChanged()
                        // 可以在修改适配器绑定的数组后，不用重新刷新Activity，通知Activity更新ListView
                        // 动态更新listView;
                    }
                }).create().show();
    }

    public QuestionsAdapter(Context context, List<Question> questionsList, MultiSelector mSelectedList) {
        this.mContext = context;
        this.mQuestionsList = questionsList;
        this.mSelector = mSelectedList;
    }

    private void setSelectable(Boolean isSelectable) {
        mSelector.setSelectable(isSelectable);
        this.notifyDataSetChanged();//对于listView进行动态的更新
    }

    public boolean isSelectable() {
        return mSelector.isSelectable();
    }

    public void setQuestionsList(List<Question> QuestionsList) {
        this.mQuestionsList = QuestionsList;
    }

    @Override
    public int getItemCount() {
        return mQuestionsList.size();
    }

    @Override
    public QuestionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_question, parent, false);
        return new QuestionViewHolder(itemView);
    }

    @Override
    public long getItemId(int position) {
        return mQuestionsList.get(position).ID;
    }

    @Override
    public void onBindViewHolder(final QuestionViewHolder holder, final int position) {
        final Question questionItem = mQuestionsList.get(position);
        holder.titleView.setText(questionItem.subject);
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(questionItem.reviewDate);
        String dateTime;
        //if today ,set time .else set date
        if (TimeUtils.isSameDay(calendar, Calendar.getInstance())) {
            dateTime = TimeUtils.formatTime(questionItem.reviewDate);
        } else {
            dateTime = TimeUtils.formatDate(questionItem.reviewDate);
        }
        holder.dateView.setText(dateTime);
        boolean itemSelected = isItemSelected(getItemId(position));

        if (itemSelected) {
            holder.itemView.setBackgroundColor(
                    mContext.getResources().getColor(R.color.black_transparent));
        } else {
            holder.itemView.setBackgroundColor(
                    mContext.getResources().getColor(R.color.white_light));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSelectable()) {
                    selectItem(position);
                } else {
                    enterQuestionDetail(questionItem);
                }
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!isMultiMode) {
                    RxBus.getDefault().post(new ChoiceModeEvent(true, mActionSelectMode));
                }
                selectItem(position);
                return true;
            }

        });
    }

    private void enterQuestionDetail(Question questionItem) {
        Intent intent = new Intent(mContext, QuestionDetailActivity.class);
        intent.putExtra("question", questionItem);
        intent.putExtra("current", mQuestionsList.indexOf(questionItem));
        intent.putExtra("question_list", (Serializable) mQuestionsList);
        RxBus.getDefault().post(new EnterActivityEvent(intent));
    }

    private void selectItem(int position) {
        boolean itemSelected = isItemSelected(getItemId(position));
        setItemSelected(position, !itemSelected);
        notifyItemChanged(position);
    }

    private void setItemSelected(int position, boolean isSelected) {
        if (isSelected) {
            mSelector.add(getItemId(position));
        } else {
            mSelector.remove(getItemId(position));
            if (mSelector.isEmpty()) {
                RxBus.getDefault().post(new ChoiceModeEvent(false, null));
            }
        }
        int size = mSelector.size();
        if (size > 0) {
            RxBus.getDefault().post(new ChoiceCountEvent(String.valueOf(size)));
        }
    }

    private boolean isItemSelected(long itemId) {
        return mSelector.contains(itemId);
    }


    class QuestionViewHolder extends RecyclerView.ViewHolder {
        private TextView titleView;
        private TextView dateView;

        public QuestionViewHolder(View itemView) {
            super(itemView);
            titleView = (TextView) itemView.findViewById(R.id.title);
            dateView = (TextView) itemView.findViewById(R.id.date);
        }
    }
}
