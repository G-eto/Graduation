package com.buct.graduation.service.impl;

import com.buct.graduation.mapper.*;
import com.buct.graduation.model.pojo.*;
import com.buct.graduation.model.pojo.recruit.Interview;
import com.buct.graduation.model.pojo.recruit.Resume;
import com.buct.graduation.model.vo.Apply;
import com.buct.graduation.model.vo.UserVData;
import com.buct.graduation.service.UserService;
import com.buct.graduation.util.GlobalName;
import com.buct.graduation.util.Utils;
import com.buct.graduation.util.spider.SpiderWOS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.rmi.CORBA.Util;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StationMapper stationMapper;
    @Autowired
    private ResumeMapper resumeMapper;
    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private PatentMapper patentMapper;
    @Autowired
    private ConferencePaperMapper conferencePaperMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private UserArticleMapper userArticleMapper;
    @Autowired
    private JournalMapper journalMapper;
    @Autowired
    private ReporterMapper reporterMapper;
    @Autowired
    private InterviewMapper interviewMapper;

    @Override
    public int register(User user) {
        user.setLevel(GlobalName.newGay);
        return userMapper.addUser(user);
    }

    @Override
    public User login(String email, String psw) {
        User user = userMapper.findUserByEmail(email);
        if(user == null || !user.getLevel().equals(GlobalName.user_type_user))
            return null;
        if(psw.equals(user.getPsw()))
            return user;
        return null;
    }

    @Override
    public int changePsw(User user) {
        return userMapper.changePsw(user);
    }

    @Override
    public User findUserById(int id) {
        return userMapper.findUserById(id);
    }

    @Override
    public UserVData findUserByUid(int uid) {
        UserVData data = new UserVData();
        data.setUser(userMapper.findUserById(uid));
        data.setArticles(articleMapper.findByIds(uid));
        data.setPapers(conferencePaperMapper.findByUid(uid));
        data.setProjects(projectMapper.findByUid(uid));
        data.setPatents(patentMapper.findByUid(uid));
        data.calculateScore();
        return data;
    }

    @Override
    public List<UserVData> findUserByLevel(String level) {
        List<User> users = userMapper.findUserByLevel(level);
        List<UserVData> data = new ArrayList<>();
        for(User user: users){
            UserVData u = new UserVData();
            u.setUser(user);
            u.setA_num(articleMapper.findByIds(user.getId()).size());
            u.setC_num(conferencePaperMapper.findByUid(user.getId()).size());
            u.setPa_num(patentMapper.findByUid(user.getId()).size());
            u.setPr_num(projectMapper.findByUid(user.getId()).size());
            data.add(u);
        }
        return data;
    }

    @Override
    public User findUserByEmail(String email) {
        return userMapper.findUserByEmail(email);
    }

    @Override
    public List<User> findUsersByStatus(String status) {
        return userMapper.findUserByStatus(status);
    }

    @Override
    @Transactional
    public int updateUser(User user) {
        User u = userMapper.findUserById(user.getId());
        //该不该删除呢
        if(user.getResumePath() != null && ((u.getResumePath() != null && !u.getResumePath().equals("")) && !user.getResumePath().equals(u.getResumePath()))){
            boolean re = Utils.deleteFile(GlobalName.ABSOLUTE_ROOT_PATH+u.getResumePath());
            if(!re){
                return -1;
            }
        }
        if(user.getPicPath() != null && ((u.getPicPath() != null && !u.getPicPath().equals("")) && !user.getPicPath().equals(u.getPicPath()))){
            boolean re = Utils.deleteFile(GlobalName.ABSOLUTE_ROOT_PATH+u.getPicPath());
            if(!re){
                return -1;
            }
        }
        return userMapper.updateUser(user);
    }

    @Override
    @Transactional//事务
    public String postResume(int uid, int sid) {
        if(resumeMapper.findByUid_Sid(uid, sid) != null){
            return "exist";
        }
        User user = userMapper.findUserById(uid);
        //todo 评价获得reporter id
        List<ConferencePaper> papers = conferencePaperMapper.findByUid(uid);
        List<Project> projects = projectMapper.findByUid(uid);
        List<Patent> patents = patentMapper.findByUid(uid);
        List<Article> articles = articleMapper.findByIds(uid);
        /*for(Article article: articles){
            if(article.getJid() == null || article.getJid() == 0){
                continue;
            }
            article.setJournal(journalMapper.findById(article.getJid()));
        }*/
        Reporter reporter = Utils.getScore(new Reporter(), projects, patents, articles, papers);
        reporter.setUid(uid);
        reporter.setTimestamp(Utils.getDate().toString());
        reporterMapper.insert(reporter);
//System.out.println("rid"+reporter.getId());
        Resume resume = new Resume();
        resume.setUid(uid);
        resume.setSid(sid);
        resume.setRid(reporter.getId());
        resume.setResumePath(user.getResumePath());
        resume.setStatus(GlobalName.resume_wait);

        return resumeMapper.addResume(resume) > 0 ? GlobalName.success : GlobalName.fail;
    }

    @Override
    public List<Project> showProjects(int uid) {
        return projectMapper.findByUid(uid);
    }

    @Override
    public List<Project> showProjectsByStatus(int uid, boolean isChecked) {
        return projectMapper.findByUidStatus(uid, isChecked);
    }

    @Override
    public List<ConferencePaper> showConferencePapers(int uid) {
        return conferencePaperMapper.findByUid(uid);
    }

    @Override
    public List<Patent> showPatents(int uid) {
        return patentMapper.findByUid(uid);
    }

    @Override
    @Transactional
    public List<UserArticle> showArticles(int uid) {
        List<Article> articles = articleMapper.findByIds(uid);
        List<UserArticle> list = new ArrayList<>();
        for(Article article: articles){
            /*if(article.getJid() != null) {
                article.setJournal(journalMapper.findById(article.getJid()));
            }else {
                article.setJournal(new Journal());
            }*/
            UserArticle a = userArticleMapper.findById(article.getId(), uid);
            a.setArticle(article);
            list.add(a);
        }
        return list;
    }

    @Override
    public int updateProject(Project project) {
        return projectMapper.update(project);
    }

    @Override
    public String addProject(Project project) {
        if(projectMapper.findByNumber(project.getNumber()) != null){
            return "此项目已存在";
        }
        return projectMapper.addProject(project) > 0 ? "保存成功": "保存失败";
    }

    @Override
    public boolean deleteProject(int id, int uid) {
        if(projectMapper.findById(id).getUid() != uid)
            return false;
        projectMapper.delete(id);
        return true;
    }

    @Override
    public int updateConferencePaper(ConferencePaper conferencePaper) {
        return conferencePaperMapper.update(conferencePaper);
    }

    @Override
    public int addConferencePaper(ConferencePaper conferencePaper) {
        SpiderWOS wos = new SpiderWOS();
        Article article = wos.getESIandtimes(conferencePaper.getName());
        if (article != null) {
            conferencePaper.setCitation(article.getCitation());
            conferencePaper.setEsi(article.getESI());
        }
        return conferencePaperMapper.addPaper(conferencePaper);
    }

    @Override
    public boolean deleteConferencePaper(int id, int uid) {
        if(conferencePaperMapper.findById(id).getUid() != uid)
            return false;
        conferencePaperMapper.delete(id);
        return true;
    }

    @Override
    public int updatePatent(Patent patent) {
        return patentMapper.update(patent);
    }

    @Override
    public int addPatent(Patent patent) {
        return patentMapper.add(patent);
    }

    @Override
    public boolean deletePatent(int id, int uid) {
        if(patentMapper.findById(id).getUid() != uid)
            return false;
        patentMapper.delete(id);
        return true;
    }

    @Override
    public int updateUserArticle(int aid, Article article, UserArticle userArticle) {
        if(userArticle == null){
            return 0;
        }
        if(aid != article.getId()){
            UserArticle userArticle2 = userArticleMapper.findById(aid, userArticle.getUid());
            userArticleMapper.deleteById(userArticle2.getId());
            return -2;
        }
        UserArticle userArticle1 = userArticleMapper.findById(userArticle.getAid(), userArticle.getUid());
        if(userArticle1 == null){
            return 0;
        }
        userArticle.setFlag(userArticle1.getFlag());
        return userArticleMapper.update(userArticle);
    }

    @Override
    @Transactional
    public int addUserArticle(Article article, UserArticle userArticle) {
        Article a = articleMapper.findByName(article.getName());
        if(a == null) {
            article.setUploadEmail(userMapper.findUserById(userArticle.getUid()).getEmail());
            articleMapper.insertArticle(article);
        }
        article = articleMapper.findByName(article.getName());
        userArticle.setAid(article.getId());
        if(userArticleMapper.findById(userArticle.getAid(), userArticle.getUid()) != null){
            return 0;
        }
        return userArticleMapper.add(userArticle);
    }

    @Override
    public boolean deleteUserArticle(int aid, int uid) {
        if(userArticleMapper.findById(aid, uid) == null) {
            return false;
        }
        userArticleMapper.delete(uid, aid);
        return true;
    }

    @Override
    @Transactional
    public List<Apply> findApply(int uid) {
        List<Resume> resumes = resumeMapper.findResumeByUid(uid);
        List<Apply> applies = new ArrayList<>();
        for(Resume resume: resumes){
            Apply apply = new Apply();
            apply.setResume(resume);
            apply.setStation(stationMapper.findById(resume.getSid()));
            apply.setReporter(reporterMapper.findById(resume.getRid()));
            apply.setInterviews(interviewMapper.findByRid(resume.getId()));
            applies.add(apply);
        }
        return applies;
    }

    @Override
    @Transactional
    public List<Apply> findComingInterview(int uid) {
        List<Resume> resumes = resumeMapper.findResumeByUidStatus(uid, GlobalName.resume_processing);
        List<Apply> applies = new ArrayList<>();
        for(Resume resume: resumes){
            Apply apply = new Apply();
            apply.setResume(resume);
            apply.setStation(stationMapper.findById(resume.getSid()));
            apply.setReporter(reporterMapper.findById(resume.getRid()));
            apply.setInterviews(interviewMapper.findByRidStatus(resume.getId(), GlobalName.interview_wait));
            applies.add(apply);
        }
        applies.removeIf(apply -> apply.getInterviews().size() == 0 || apply.getInterviews().get(0).getTime().compareTo(Utils.getDate().toDate()) < 0);
        return applies;
    }

    @Override
    public ConferencePaper findPaperById(int id) {
        return conferencePaperMapper.findById(id);
    }

    @Override
    public List<Resume> findComingInterviews() {
        List<Interview> interviews = interviewMapper.findComing();
        List<Resume> resumes = new ArrayList<>();
        for(Interview interview: interviews){
            Resume resume = resumeMapper.findById(interview.getRid());
            resume.getInterviews().add(interview);
            resume.setStation(stationMapper.findById(resume.getSid()));
            resume.setUser(userMapper.findUserById(resume.getUid()));
            resume.setReporter(reporterMapper.findById(resume.getRid()));
            resumes.add(resume);
        }
        return resumes;
    }
}
