package com.buct.graduation.model.pojo;

/**
 * 考虑到一篇论文可能有多个作者
 * 遂新建UA表
 */
public class UserArticle {
    private Integer id;
    private Integer uid;//用户id
    private Integer aid;//期刊论文id
    private String role;//担任角色
    private String notes;
    private Article article;
    private String flag;//GlobalName.teacher_//待认领等状态标识

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public Article getArticle() {
        return article;
    }

    public void setArticle(Article article) {
        this.article = article;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public Integer getAid() {
        return aid;
    }

    public void setAid(Integer aid) {
        this.aid = aid;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
