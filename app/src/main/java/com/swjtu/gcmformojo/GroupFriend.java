package com.swjtu.gcmformojo;

import java.util.List;

/**
 * Created by HeiPi on 2017/2/21.
 */

public class GroupFriend {
    private String groupName;// 大组名称
    private List<Friend> groupChild;// 对应大组的小组成员对象数组

    public GroupFriend() {
        super();
    }

    public GroupFriend(String groupName, List<Friend> groupChild) {
        super();
        this.groupName = groupName;
        this.groupChild = groupChild;
    }

    public void add(Friend u) {// 往小组中添加用户
        groupChild.add(u);
    }

    public void remove(Friend u) {// 根据用户对象移除用户
        groupChild.remove(u);
    }

    public void remove(int index) {// 根据下标移除用户
        groupChild.remove(index);
    }

    public int getChildSize() {// 小组的大小
        return groupChild.size();
    }

    public Friend getChild(int index) {// 根据下标得到用户
        return groupChild.get(index);
    }

    // get...set...
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<Friend> getGroupChild() {
        return groupChild;
    }

    public void setGroupChild(List<Friend> groupChild) {
        this.groupChild = groupChild;
    }
}
