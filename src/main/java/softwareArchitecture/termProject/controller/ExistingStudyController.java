package softwareArchitecture.termProject.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import softwareArchitecture.termProject.domain.*;
import softwareArchitecture.termProject.repository.MemberStudyRepository;
import softwareArchitecture.termProject.repository.StudySearch;
import softwareArchitecture.termProject.service.ExistingStudyService;
import softwareArchitecture.termProject.service.StudyService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.Null;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
@SessionAttributes("member")
public class ExistingStudyController {

    private final ExistingStudyService existingStudyService;
    private final StudyService studyService;

    @GetMapping("/studyList")
    public String studyList(
            @ModelAttribute("studySearch") StudySearch studySearch,
            Model model
    ) {

        List<Study> studies;
        if (studySearch == null || (studySearch.getStudyName() == null && studySearch.getCategory() == null))
            studies = existingStudyService.showAllStudies();
        else
            studies = existingStudyService.findStudies(studySearch);

        model.addAttribute("loginForm", new LoginForm());
        model.addAttribute("studies", studies);
        return "studyList";
    }

//    @GetMapping("/categoriedStudies")
//    public String categoriedStudies(@ModelAttribute("studySearch") StudySearch studySearch, Model model) {
//        List<Study> categoriedStudies = existingStudyService.findStudies(studySearch);
//        model.addAttribute("categoriedStudies", categoriedStudies);
//        return "study/categoriedStudies";
//    }


    @PostMapping("/studies/{studyId}/participate")
    public String participate(
            @PathVariable("studyId") Long studyId,
            HttpServletRequest request
    ) {
        HttpSession session = request.getSession();
        Long memberId = (Long)session.getAttribute("memberId");

        Study study = existingStudyService.findStudy(studyId);

        if (study.getPresent()==study.getCapacity()) {
            return "redirect:/studyList";
        }
        else {
            existingStudyService.participateStudy(memberId, studyId);
            return "redirect:/studyList";
        }
    }

    @PostMapping(value = "/studies/{studyId}/quit")
    public String quit(@PathVariable("studyId") Long studyId, HttpServletRequest request) {

        HttpSession session = request.getSession();
        Long memberId = (Long)session.getAttribute("memberId");

        Long memberStudyId = existingStudyService.findMemberStudy(memberId, studyId);
        existingStudyService.quitStudy(memberStudyId, memberId);
        return "redirect:/studyList";
    }

    @GetMapping(value = "/studies/{studyId}/detail")
    public String showDetail(@PathVariable("studyId") Long studyId, Model model, HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new NullPointerException("???????????? ????????? ????????? ?????????.");
        }

        Study study = studyService.findOne(studyId);
        model.addAttribute("study", study);

        String statusString = "";
        switch (study.getStudyStatus()) {
            case DELETED:
                statusString = "?????????";
                break;
            case COMPLETE:
                statusString = "?????? ??????";
                break;
            case RECRUITING:
                statusString = "?????? ???";
                break;
        }
        model.addAttribute("status", statusString);

        HttpSession session = request.getSession();
        Long memberId = (Long)session.getAttribute("memberId");

        List<MemberStudy> memberStudies = existingStudyService.showMyStudies(memberId);
        int type = 0;
        Long memberStudyId = null;

        for (MemberStudy memberStudy : memberStudies) {
            if (Objects.equals(memberStudy.getStudy().getId(), studyId)) {
                if (memberStudy.getMemberStudyStatus() == MemberStudyStatus.REJECTED)
                    continue;

                memberStudyId = memberStudy.getId();
                type = memberStudy.isOwner() ? 2 : 1;
                break;
            }
        }

        boolean disabled = study.getStudyStatus() == StudyStatus.DELETED;

        List<Member> members = existingStudyService.findMembers(studyId);
        model.addAttribute("members", members);


        model.addAttribute("memberStudyId", memberStudyId);
        model.addAttribute("type", type);
        model.addAttribute("disabled", disabled);

        String category_name = null;
        switch (study.getCategory()) {
            case SPRING: category_name = "?????????"; break;
            case NETWORK: category_name = "????????????"; break;
            case NODEJS: category_name = "Node.JS"; break;
            case OS: category_name = "OS"; break;
            case DATA_STRUCTURE: category_name = "????????????"; break;
            case DATABASE: category_name = "DB"; break;
        }
        model.addAttribute("category_name", category_name);

        return "studyDetail";
    }



    @ExceptionHandler
    public String cookieHandler(NullPointerException exception) {
        return "redirect:/studyList";
    }

}
