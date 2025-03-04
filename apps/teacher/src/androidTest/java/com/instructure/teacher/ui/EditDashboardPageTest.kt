/*
 * Copyright (C) 2017 - present Instructure, Inc.
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

package com.instructure.teacher.ui

import com.instructure.canvas.espresso.mockCanvas.MockCanvas
import com.instructure.canvas.espresso.mockCanvas.init
import com.instructure.espresso.TestRail
import com.instructure.teacher.ui.utils.TeacherTest
import com.instructure.teacher.ui.utils.tokenLogin
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class EditDashboardPageTest : TeacherTest() {

    @Test
    @TestRail(ID = "C3109572")
    override fun displaysPageObjects() {
        setUpAndSignIn(numCourses = 1, numFavoriteCourses = 0)
        coursesListPage.openEditDashboard()
        editDashboardPage.assertPageObjects()
    }

    @Test
    @TestRail(ID = "C3109572")
    fun displaysCourseList() {
        val data = setUpAndSignIn(numCourses = 1, numFavoriteCourses = 0)
        val course = data.courses.values.toList()[0]
        coursesListPage.openEditDashboard()
        editDashboardPage.assertHasCourse(course.name)
    }

    @Test
    @TestRail(ID = "C3109574")
    fun addCourseToFavourites() {
        val data = setUpAndSignIn(numCourses = 1, numFavoriteCourses = 0)
        val courses = data.courses.values.toList()

        coursesListPage.openEditDashboard()
        editDashboardPage.toggleFavouringCourse(courses[0].name)
        editDashboardPage.assertCourseFavoured(courses[0])
    }

    @Test
    @TestRail(ID = "C3109575")
    fun removeCourseFromFavourites() {
        val data = setUpAndSignIn(numCourses = 1, numFavoriteCourses = 1)
        val courses = data.courses.values.toList()

        coursesListPage.openEditDashboard()
        editDashboardPage.toggleFavouringCourse(courses[0].name)
        editDashboardPage.assertCourseUnfavoured(courses[0])
    }

    private fun setUpAndSignIn(numCourses: Int = 1, numFavoriteCourses: Int = 1) : MockCanvas {
        val data = MockCanvas.init(teacherCount = 1, courseCount = numCourses, favoriteCourseCount = numFavoriteCourses)
        val teacher = data.teachers[0]
        val token = data.tokenFor(teacher)!!
        tokenLogin(data.domain, token, teacher)
        return data
    }
}
