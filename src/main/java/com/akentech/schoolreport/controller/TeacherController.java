package com.akentech.schoolreport.controller;

import com.akentech.schoolreport.exception.EntityNotFoundException;
import com.akentech.schoolreport.model.Teacher;
import com.akentech.schoolreport.repository.SubjectRepository;
import com.akentech.schoolreport.repository.ClassRoomRepository;
import com.akentech.schoolreport.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/teachers")
@RequiredArgsConstructor
@Slf4j
public class TeacherController {

    private final TeacherService teacherService;
    private final SubjectRepository subjectRepository;
    private final ClassRoomRepository classRoomRepository;

    @GetMapping
    public String listTeachers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) Long subjectId,
            Model model) {

        try {
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Teacher> teacherPage = teacherService.getTeachersByFilters(firstName, lastName, subjectId, pageable);

            model.addAttribute("teachers", teacherPage.getContent());
            model.addAttribute("currentPage", teacherPage.getNumber());
            model.addAttribute("totalPages", teacherPage.getTotalPages());
            model.addAttribute("totalItems", teacherPage.getTotalElements());
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);

            // Add filter values for form persistence
            model.addAttribute("firstNameFilter", firstName);
            model.addAttribute("lastNameFilter", lastName);
            model.addAttribute("subjectIdFilter", subjectId);

            return "teachers";
        } catch (Exception e) {
            log.error("Error loading teachers list", e);
            model.addAttribute("error", "Unable to load teachers");
            return "teachers";
        }
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        Teacher teacher = new Teacher();
        teacher.setTeacherId(teacherService.generateTeacherId());
        model.addAttribute("teacher", teacher);
        model.addAttribute("allSubjects", subjectRepository.findAll());
        model.addAttribute("allClassrooms", classRoomRepository.findAll());
        return "add-teacher";
    }

    @PostMapping("/add")
    public String addTeacher(@ModelAttribute Teacher teacher,
                             @RequestParam(value = "subjectIds", required = false) List<Long> subjectIds,
                             @RequestParam(value = "classroomIds", required = false) List<Long> classroomIds) {
        try {
            teacherService.createTeacher(teacher, subjectIds, classroomIds);
            return "redirect:/teachers?success";
        } catch (Exception e) {
            log.error("Error adding teacher", e);
            return "redirect:/teachers?error=add_failed";
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        try {
            Teacher teacher = teacherService.getTeacherByIdOrThrow(id);
            model.addAttribute("teacher", teacher);
            model.addAttribute("allSubjects", subjectRepository.findAll());
            model.addAttribute("allClassrooms", classRoomRepository.findAll());
            return "edit-teacher";
        } catch (EntityNotFoundException e) {
            log.warn("Teacher not found with id: {}", id);
            return "redirect:/teachers?error=notfound";
        } catch (Exception e) {
            log.error("Error loading teacher edit form for id: {}", id, e);
            return "redirect:/teachers?error=server_error";
        }
    }

    @PostMapping("/update")
    public String updateTeacher(@ModelAttribute Teacher teacher,
                                @RequestParam(value = "subjectIds", required = false) List<Long> subjectIds,
                                @RequestParam(value = "classroomIds", required = false) List<Long> classroomIds) {
        try {
            teacherService.updateTeacher(teacher.getId(), teacher, subjectIds, classroomIds);
            return "redirect:/teachers?updated";
        } catch (EntityNotFoundException e) {
            log.warn("Teacher not found for update with id: {}", teacher.getId());
            return "redirect:/teachers?error=notfound";
        } catch (Exception e) {
            log.error("Error updating teacher with id: {}", teacher.getId(), e);
            return "redirect:/teachers?error=update_failed";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteTeacher(@PathVariable Long id) {
        try {
            teacherService.deleteTeacher(id);
            return "redirect:/teachers?deleted";
        } catch (EntityNotFoundException e) {
            log.warn("Teacher not found for deletion with id: {}", id);
            return "redirect:/teachers?error=notfound";
        } catch (Exception e) {
            log.error("Error deleting teacher with id: {}", id, e);
            return "redirect:/teachers?error=delete_failed";
        }
    }

    @GetMapping("/view/{id}")
    public String viewTeacher(@PathVariable Long id, Model model) {
        try {
            Teacher teacher = teacherService.getTeacherByIdOrThrow(id);
            model.addAttribute("teacher", teacher);
            return "view-teacher";
        } catch (EntityNotFoundException e) {
            log.warn("Teacher not found for viewing with id: {}", id);
            return "redirect:/teachers?error=notfound";
        } catch (Exception e) {
            log.error("Error viewing teacher with id: {}", id, e);
            return "redirect:/teachers?error=server_error";
        }
    }
}