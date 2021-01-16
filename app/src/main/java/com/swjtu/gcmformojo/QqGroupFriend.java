package com.swjtu.gcmformojo;

import java.util.List;

/**
 * Created by HeiPi on 2017/2/21.
 */

public class QqGroupFriend {
    private String groupName;// 分组名称
    private List<QqFriend> groupChild;// 对应分组的小组成员对象数组

    public QqGroupFriend() {
        super();
    }

    public QqGroupFriend(String groupName, List<QqFriend> groupChild) {
        super();
        this.groupName = groupName;
        this.groupChild = groupChild;
    }

    public void add(QqFriend u) {// 往小组中添加用户
        groupChild.add(u);
    }

    public void remove(QqFriend u) {// 根据用户对象移除用户
        groupChild.remove(u);
    }

    public void remove(int index) {// 根据下标移除用户
        groupChild.remove(index);
    }

    public int getChildSize() {// 小组的大小
        return groupChild.size();
    }

    public QqFriend getChild(int index) {// 根据下标得到用户
        return groupChild.get(index);
    }

    // get...set...
    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<QqFriend> getGroupChild() {
        return groupChild;
    }

    public void setGroupChild(List<QqFriend> groupChild) {
        this.groupChild = groupChild;
    }
}
