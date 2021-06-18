package com.swjtu.gcmformojo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * 微信好友数据显示
 * Created by Think on 2018/2/4.
 * 这是从QqFriendAdapter直接复制来的，可能会有一堆bug或者无用代码
 */

public class WechatFriendAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<WechatFriendGroup> group;
    public WechatFriendAdapter(Context context, List<WechatFriendGroup> group) {
        super();
        this.context = context;
        this.group = group;
    }

    //得到大组成员总数
    @Override
    public int getGroupCount() {
        return group.size();
    }

    //得到小组成员的数量
    @Override
    public int getChildrenCount(int groupPosition) {
        return group.get(groupPosition).getChildSize();
    }

    //得到大组成员名称
    @Override
    public Object getGroup(int groupPosition) {
        return group.get(groupPosition).getGroupName();
    }

    //// 得到小组成员的名称
    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return group.get(groupPosition).getChild(childPosition);
    }

    //得到大组成员的id
    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    // 得到小组成员id
    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.wechat_friend_group_items, null);
        }

        TextView title = (TextView) convertView.findViewById(R.id.wechat_friend_group_name);
        title.setText(getGroup(groupPosition).toString());// 设置大组成员名称

        return convertView;
    }

    @Override
    // 得到小组成员的view
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.wechat_friend_child_items, null);
        }
        final TextView WechatFriendNameView = (TextView) convertView
                .findViewById(R.id.wechat_friend_child_name);// 显示好友名

        // openwx自带displayname，优先返回备注，如无备注即返回用户名，故无需进行name/markname处理
        // 由于腾讯新闻的displayname默认为昵称未知，故特殊处理
        final String displayname = group.get(groupPosition).getChild(childPosition)
                .get_displayname();
        final String id = group.get(groupPosition).getChild(childPosition)
                .get_id();
        if (id.equals("newsapp")) {
            WechatFriendNameView.setText("\t\t" + context.getString(R.string.text_wechat_contacts_newsapp) + context.getString(R.string.text_wechat_contacts_unable_to_send_msg));
        } else {
            WechatFriendNameView.setText("\t\t"+displayname);
        }

        return convertView;
    }

    @Override
    // 得到小组成员是否被选择
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
