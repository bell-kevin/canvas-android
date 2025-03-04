/*
 * Copyright (C) 2020 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.instructure.student.ui.e2e

import android.os.SystemClock.sleep
import android.util.Log
import androidx.test.espresso.Espresso
import androidx.test.rule.GrantPermissionRule
import com.instructure.canvas.espresso.E2E
import com.instructure.dataseeding.api.AssignmentsApi
import com.instructure.dataseeding.api.SubmissionsApi
import com.instructure.dataseeding.model.FileUploadType
import com.instructure.dataseeding.model.GradingType
import com.instructure.dataseeding.model.SubmissionType
import com.instructure.dataseeding.util.days
import com.instructure.dataseeding.util.fromNow
import com.instructure.dataseeding.util.iso8601
import com.instructure.panda_annotations.FeatureCategory
import com.instructure.panda_annotations.Priority
import com.instructure.panda_annotations.TestCategory
import com.instructure.panda_annotations.TestMetaData
import com.instructure.student.ui.utils.StudentTest
import com.instructure.student.ui.utils.seedData
import com.instructure.student.ui.utils.tokenLogin
import com.instructure.student.ui.utils.uploadTextFile
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class AssignmentsE2ETest: StudentTest() {
    override fun displaysPageObjects() = Unit

    override fun enableAndConfigureAccessibilityChecks() {
        //We don't want to see accessibility errors on E2E tests
    }

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
    )

    @E2E
    @Test
    @TestMetaData(Priority.MANDATORY, FeatureCategory.ASSIGNMENTS, TestCategory.E2E)
    fun testPointsGradeTextAssignmentE2E() {

        Log.d(PREPARATION_TAG,"Seeding data.")
        val data = seedData(students = 1, teachers = 1, courses = 1)
        val student = data.studentsList[0]
        val teacher = data.teachersList[0]
        val course = data.coursesList[0]

        Log.d(PREPARATION_TAG,"Seeding 'Text Entry' assignment for ${course.name} course.")
        val pointsTextAssignment = AssignmentsApi.createAssignment(AssignmentsApi.CreateAssignmentRequest(
                courseId = course.id,
                submissionTypes = listOf(SubmissionType.ONLINE_TEXT_ENTRY),
                gradingType = GradingType.POINTS,
                teacherToken = teacher.token,
                pointsPossible = 15.0,
                dueAt = 1.days.fromNow.iso8601
        ))

        Log.d(STEP_TAG, "Login with user: ${student.name}, login id: ${student.loginId}.")
        tokenLogin(student)
        dashboardPage.waitForRender()

        Log.d(STEP_TAG,"Select course: ${course.name}.")
        dashboardPage.selectCourse(course)

        Log.d(STEP_TAG,"Navigate to course Assignments Page.")
        courseBrowserPage.selectAssignments()

        Log.d(STEP_TAG,"Verify that our assignments are present, along with any grade/date info. Click on assignment ${pointsTextAssignment.name}.")
        assignmentListPage.assertHasAssignment(pointsTextAssignment)
        assignmentListPage.clickAssignment(pointsTextAssignment)

        Log.d(PREPARATION_TAG,"Submit assignment: ${pointsTextAssignment.name} for student: ${student.name}.")
        SubmissionsApi.submitCourseAssignment(
                submissionType = SubmissionType.ONLINE_TEXT_ENTRY,
                courseId = course.id,
                assignmentId = pointsTextAssignment.id,
                studentToken = student.token,
                fileIds = emptyList<Long>().toMutableList()
        )

        Log.d(STEP_TAG,"Refresh the page, and assert that the assignment ${pointsTextAssignment.name} has been submitted successfully.")
        assignmentDetailsPage.refresh()
        assignmentDetailsPage.assertAssignmentSubmitted()

        Log.d(PREPARATION_TAG,"Grade submission: ${pointsTextAssignment.name} with 13 points.")
        val textGrade = SubmissionsApi.gradeSubmission(
                teacherToken = teacher.token,
                courseId = course.id,
                assignmentId = pointsTextAssignment.id,
                studentId = student.id,
                postedGrade = "13",
                excused = false
        )

        Log.d(STEP_TAG,"Refresh the page. Assert that the assignment ${pointsTextAssignment.name} has been graded with 13 points.")
        assignmentDetailsPage.refresh()
        assignmentDetailsPage.verifyAssignmentGraded("13")

        Log.d(STEP_TAG,"Navigate back to Assignments Page and assert that the assignment ${pointsTextAssignment.name} can be seen there with the corresponding grade.")
        Espresso.pressBack()
        assignmentListPage.refresh()
        assignmentListPage.assertHasAssignment(pointsTextAssignment, textGrade.grade)

    }

    @E2E
    @Test
    @TestMetaData(Priority.MANDATORY, FeatureCategory.ASSIGNMENTS, TestCategory.E2E)
    fun testLetterGradeTextAssignmentE2E() {

        Log.d(PREPARATION_TAG,"Seeding data.")
        val data = seedData(students = 1, teachers = 1, courses = 1)
        val student = data.studentsList[0]
        val teacher = data.teachersList[0]
        val course = data.coursesList[0]

        Log.d(PREPARATION_TAG,"Seeding 'Text Entry' assignment for ${course.name} course.")
        val letterGradeTextAssignment = AssignmentsApi.createAssignment(AssignmentsApi.CreateAssignmentRequest(
                courseId = course.id,
                submissionTypes = listOf(SubmissionType.ONLINE_TEXT_ENTRY),
                gradingType = GradingType.LETTER_GRADE,
                teacherToken = teacher.token,
                pointsPossible = 20.0
        ))

        Log.d(PREPARATION_TAG,"Submit assignment: ${letterGradeTextAssignment.name} for student: ${student.name}.")
        SubmissionsApi.seedAssignmentSubmission(SubmissionsApi.SubmissionSeedRequest(
                assignmentId = letterGradeTextAssignment.id,
                courseId = course.id,
                studentToken = student.token,
                submissionSeedsList = listOf(SubmissionsApi.SubmissionSeedInfo(
                        amount = 1,
                        submissionType = SubmissionType.ONLINE_TEXT_ENTRY
                ))
        ))

        Log.d(PREPARATION_TAG,"Grade submission: ${letterGradeTextAssignment.name} with 13 points.")
        val submissionGrade = SubmissionsApi.gradeSubmission(
                teacherToken = teacher.token,
                courseId = course.id,
                assignmentId = letterGradeTextAssignment.id,
                studentId = student.id,
                postedGrade = "16",
                excused = false
        )

        Log.d(STEP_TAG, "Login with user: ${student.name}, login id: ${student.loginId}.")
        tokenLogin(student)
        dashboardPage.waitForRender()

        Log.d(STEP_TAG,"Select ${course.name} course and navigate to it's Assignments Page.")
        dashboardPage.selectCourse(course)
        courseBrowserPage.selectAssignments()

        Log.d(STEP_TAG,"Assert that ${letterGradeTextAssignment.name} assignment is displayed with the corresponding grade: ${submissionGrade.grade}.")
        assignmentListPage.assertHasAssignment(letterGradeTextAssignment, submissionGrade.grade)

    }

    @E2E
    @Test
    @TestMetaData(Priority.MANDATORY, FeatureCategory.ASSIGNMENTS, TestCategory.E2E)
    fun testPercentageFileAssignmentWithCommentE2E() {

        Log.d(PREPARATION_TAG,"Seeding data.")
        val data = seedData(students = 1, teachers = 1, courses = 1)
        val student = data.studentsList[0]
        val teacher = data.teachersList[0]
        val course = data.coursesList[0]

        Log.d(PREPARATION_TAG,"Seeding assignment for ${course.name} course.")
        val percentageFileAssignment = AssignmentsApi.createAssignment(AssignmentsApi.CreateAssignmentRequest(
                courseId = course.id,
                submissionTypes = listOf(SubmissionType.ONLINE_UPLOAD),
                gradingType = GradingType.PERCENT,
                teacherToken = teacher.token,
                pointsPossible = 25.0,
                allowedExtensions = listOf("txt", "pdf", "jpg")
        ))

        Log.d(STEP_TAG, "Login with user: ${student.name}, login id: ${student.loginId}.")
        tokenLogin(student)
        dashboardPage.waitForRender()

        Log.d(STEP_TAG,"Select ${course.name} course and navigate to it's Assignments Page.")
        dashboardPage.selectCourse(course)
        courseBrowserPage.selectAssignments()

        Log.d(STEP_TAG,"Assert that ${percentageFileAssignment.name} assignment is displayed.")
        assignmentListPage.assertHasAssignment(percentageFileAssignment)

        Log.d(STEP_TAG,"Select assignment: ${percentageFileAssignment.name}.")

        Log.d(STEP_TAG,"Click on ${percentageFileAssignment.name} assignment.")
        assignmentListPage.clickAssignment(percentageFileAssignment)

        Log.d(PREPARATION_TAG, "Seed a text file.")
        val uploadInfo = uploadTextFile(
                courseId = course.id,
                assignmentId = percentageFileAssignment.id,
                token = student.token,
                fileUploadType = FileUploadType.ASSIGNMENT_SUBMISSION
        )

        Log.d(PREPARATION_TAG,"Submit ${percentageFileAssignment.name} assignment for ${student.name} student.")
        SubmissionsApi.submitCourseAssignment(
                submissionType = SubmissionType.ONLINE_UPLOAD,
                courseId = course.id,
                assignmentId = percentageFileAssignment.id,
                fileIds = listOf(uploadInfo.id).toMutableList(),
                studentToken = student.token
        )

        Log.d(STEP_TAG,"Refresh the page. Assert that the ${percentageFileAssignment.name} assignment has been submitted.")
        assignmentDetailsPage.refresh()
        assignmentDetailsPage.assertAssignmentSubmitted()

        Log.d(PREPARATION_TAG,"Grade ${percentageFileAssignment.name} assignment with 22 percentage.")
        SubmissionsApi.gradeSubmission(
                teacherToken = teacher.token,
                courseId = course.id,
                assignmentId = percentageFileAssignment.id,
                studentId = student.id,
                postedGrade = "22",
                excused = false
        )

        Log.d(STEP_TAG,"Refresh the page. Assert that the ${percentageFileAssignment.name} assignment has been graded with 22 percentage.")
        assignmentDetailsPage.refresh()
        assignmentDetailsPage.verifyAssignmentGraded("22")


        Log.d(STEP_TAG,"Navigate to submission details Comments Tab.")
        assignmentDetailsPage.goToSubmissionDetails()
        submissionDetailsPage.openComments()

        Log.d(STEP_TAG,"Assert that ${uploadInfo.fileName} file has been displayed as a comment.")
        submissionDetailsPage.assertCommentDisplayed(
                uploadInfo.fileName,
                student)

        val newComment = "My comment!!"
        Log.d(STEP_TAG,"Add a new comment ($newComment) and send it.")
        submissionDetailsPage.addAndSendComment(newComment)
        sleep(2000) // Give the comment time to propagate

        Log.d(STEP_TAG,"Assert that $newComment is displayed.")
        submissionDetailsPage.assertCommentDisplayed(newComment, student)
    }

    @E2E
    @Test
    @TestMetaData(Priority.MANDATORY, FeatureCategory.ASSIGNMENTS, TestCategory.E2E)
    fun testMultipleAssignmentsE2E() {

        Log.d(PREPARATION_TAG,"Seeding data.")
        val data = seedData(students = 1, teachers = 1, courses = 1)
        val student = data.studentsList[0]
        val teacher = data.teachersList[0]
        val course = data.coursesList[0]

        Log.d(PREPARATION_TAG,"Seeding assignment for ${course.name} course.")
        val letterGradeTextAssignment = AssignmentsApi.createAssignment(AssignmentsApi.CreateAssignmentRequest(
                courseId = course.id,
                submissionTypes = listOf(SubmissionType.ONLINE_TEXT_ENTRY),
                gradingType = GradingType.LETTER_GRADE,
                teacherToken = teacher.token,
                pointsPossible = 20.0
        ))

        Log.d(PREPARATION_TAG,"Submit ${letterGradeTextAssignment.name} assignment for ${student.name} student.")
        SubmissionsApi.seedAssignmentSubmission(SubmissionsApi.SubmissionSeedRequest(
                assignmentId = letterGradeTextAssignment.id,
                courseId = course.id,
                studentToken = student.token,
                submissionSeedsList = listOf(SubmissionsApi.SubmissionSeedInfo(
                        amount = 1,
                        submissionType = SubmissionType.ONLINE_TEXT_ENTRY
                ))
        ))

        Log.d(PREPARATION_TAG,"Grade ${letterGradeTextAssignment.name} assignment with 16.")
        SubmissionsApi.gradeSubmission(
                teacherToken = teacher.token,
                courseId = course.id,
                assignmentId = letterGradeTextAssignment.id,
                studentId = student.id,
                postedGrade = "16",
                excused = false
        )

        Log.d(PREPARATION_TAG,"Seeding assignment for ${course.name} course.")
        val pointsTextAssignment = AssignmentsApi.createAssignment(AssignmentsApi.CreateAssignmentRequest(
                courseId = course.id,
                submissionTypes = listOf(SubmissionType.ONLINE_TEXT_ENTRY),
                gradingType = GradingType.POINTS,
                teacherToken = teacher.token,
                pointsPossible = 15.0,
                dueAt = 1.days.fromNow.iso8601
        ))

        Log.d(PREPARATION_TAG,"Submit ${pointsTextAssignment.name} assignment for ${student.name} student.")
        SubmissionsApi.seedAssignmentSubmission(SubmissionsApi.SubmissionSeedRequest(
                assignmentId = pointsTextAssignment.id,
                courseId = course.id,
                studentToken = student.token,
                submissionSeedsList = listOf(SubmissionsApi.SubmissionSeedInfo(
                        amount = 1,
                        submissionType = SubmissionType.ONLINE_TEXT_ENTRY
                ))
        ))

        Log.d(PREPARATION_TAG,"Grade ${pointsTextAssignment.name} assignment with 13 points.")
        SubmissionsApi.gradeSubmission(
                teacherToken = teacher.token,
                courseId = course.id,
                assignmentId = pointsTextAssignment.id,
                studentId = student.id,
                postedGrade = "13",
                excused = false
        )

        Log.d(STEP_TAG, "Login with user: ${student.name}, login id: ${student.loginId}.")
        tokenLogin(student)
        dashboardPage.waitForRender()

        Log.d(STEP_TAG,"Select ${course.name} course and navigate to it's Assignments Page.")
        dashboardPage.selectCourse(course)
        courseBrowserPage.selectAssignments()

        Log.d(STEP_TAG,"Assert that ${pointsTextAssignment.name} assignment is displayed with the corresponding grade: 13.")
        assignmentListPage.assertHasAssignment(pointsTextAssignment,"13")

        Log.d(STEP_TAG,"Assert that ${letterGradeTextAssignment.name} assignment is displayed with the corresponding grade: 16.")
        assignmentListPage.assertHasAssignment(letterGradeTextAssignment, "16")
    }

    @E2E
    @Test
    @TestMetaData(Priority.MANDATORY, FeatureCategory.COMMENTS, TestCategory.E2E)
    fun testMediaCommentsE2E() {

        Log.d(PREPARATION_TAG,"Seeding data.")
        val data = seedData(students = 1, teachers = 1, courses = 1)
        val student = data.studentsList[0]
        val teacher = data.teachersList[0]
        val course = data.coursesList[0]

        Log.d(PREPARATION_TAG,"Seeding assignment for ${course.name} course.")
        val assignment = AssignmentsApi.createAssignment(AssignmentsApi.CreateAssignmentRequest(
                courseId = course.id,
                submissionTypes = listOf(SubmissionType.ONLINE_TEXT_ENTRY),
                gradingType = GradingType.POINTS,
                teacherToken = teacher.token,
                pointsPossible = 15.0,
                dueAt = 1.days.fromNow.iso8601
        ))

        Log.d(PREPARATION_TAG,"Submit ${assignment.name} assignment for ${student.name} student.")
        SubmissionsApi.seedAssignmentSubmission(SubmissionsApi.SubmissionSeedRequest(
                assignmentId = assignment.id,
                courseId = course.id,
                studentToken = student.token,
                submissionSeedsList = listOf(SubmissionsApi.SubmissionSeedInfo(
                        amount = 1,
                        submissionType = SubmissionType.ONLINE_TEXT_ENTRY
                ))
        ))

        Log.d(STEP_TAG, "Login with user: ${student.name}, login id: ${student.loginId}.")
        tokenLogin(student)
        dashboardPage.waitForRender()

        Log.d(STEP_TAG,"Select ${course.name} course and navigate to it's Assignments Page.")
        dashboardPage.selectCourse(course)
        courseBrowserPage.selectAssignments()

        Log.d(STEP_TAG,"Click on ${assignment.name} assignment.")
        assignmentListPage.clickAssignment(assignment)

        Log.d(STEP_TAG,"Navigate to submission details Comments Tab.")
        assignmentDetailsPage.goToSubmissionDetails()
        submissionDetailsPage.openComments()

        submissionDetailsPage.addAndSendVideoComment()
        sleep(3000) // wait for video comment submission to propagate
        submissionDetailsPage.assertVideoCommentDisplayed()

        Log.d(STEP_TAG,"Send an audio comment.")
        submissionDetailsPage.addAndSendAudioComment()
        sleep(3000) // Wait for audio comment submission to propagate

        Log.d(STEP_TAG,"Assert that the audio comment has been displayed.")
        submissionDetailsPage.assertAudioCommentDisplayed()
    }
}