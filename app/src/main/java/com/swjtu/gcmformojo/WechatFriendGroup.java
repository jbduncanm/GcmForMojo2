package com.swjtu.gcmformojo;

import java.util.List;

/**
 * 微信好友分组
 * Created by Think on 2018/2/4.
 * 由QqFriendGroup复制粘贴而来，可能会有一堆bug或者无用代码
 */

public class WechatFriendGroup {
    private String groupName;// 分组名称
    private List<WechatFriend> groupChild;// 对应分组的小组成员对象数组

    public WechatFriendGroup() {
        super();
    }

    public WechatFriendGroup(String groupName, List<WechatFriend> groupChild) {
        super();
        this.groupName = groupName;
        this.groupChild = groupChild;
    }

    public void add(WechatFriend u) {// 往小组中添加用户
        groupChild.add(u);
    }

    public void remove(WechatFriend u) {// 根据用户对象移除用户
        groupChild.remove(u);
    }

    public void remove(int index) {// 根据下标移除用户
        groupChild.remove(index);
    }

    public int getChildSize() {// 小组的大小
        return groupChild.size();
    }

    public WechatFriend getChild(int index) {// 根据下标得到用户
        return groupChild.get(index);
    }

    // get...set...
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<WechatFriend> getGroupChild() {
        return groupChild;
    }

    public void setGroupChild(List<WechatFriend> groupChild) {
        this.groupChild = groupChild;
    }
}
