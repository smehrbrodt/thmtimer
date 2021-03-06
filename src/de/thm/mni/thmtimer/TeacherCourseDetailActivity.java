package de.thm.mni.thmtimer;

import java.lang.reflect.Field;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import de.thm.mni.thmtimer.util.DepthPageTransformer;
import de.thm.mni.thmtimer.util.FixedSpeedScroller;
import de.thm.mni.thmtimer.util.ModuleDAO;
import de.thm.mni.thmtimer.util.ModuleDAOListener;
import de.thm.thmtimer.entities.Course;


public class TeacherCourseDetailActivity extends FragmentActivity implements ModuleDAOListener {
	
	private final int DAO_REQUEST_DURATION_PER_CATEGORY = 0;
	private final int DAO_REQUEST_DURATIONS_PER_WEEK = 1;
	
	private Fragment mLineChart;
	private Fragment mPieChart;
	
	private Long mCourseID;
	private Course mCourse;
	
	private ViewPager mPager;
	private FragmentPagerAdapter mPagerAdapter;
	
	private ActionBar mActionBar;
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		ModuleDAO.setNewContext(this, this);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.teachercoursedetailactivity);
		
		
		if(savedInstanceState != null) {
			
			mLineChart = getSupportFragmentManager().getFragment(savedInstanceState, "linechart");
			mPieChart  = getSupportFragmentManager().getFragment(savedInstanceState, "piechart");
		}
		
		if(mPieChart == null)
			mPieChart = new TeacherCourseDetailPiechartFragment();
		
		if(mLineChart == null)
			mLineChart = new TeacherCourseDetailLinechartFragment();
		
		
		mCourseID = getIntent().getExtras().getLong("course_id");
		mCourse   = ModuleDAO.getTeacherCourseByID(mCourseID);
		
		// Kursname setzen
		TextView courseName = (TextView)findViewById(R.id.teachercoursedetail_txtCourseName);
		courseName.setText(mCourse.getName());
		
		// Eingeschriebene Studenten setzen
		TextView enrolledStudents = (TextView)findViewById(R.id.teachercoursedetail_txtEnrolledStudents);
		enrolledStudents.setText(String.format("%s: %d",
				                               getText(R.string.students),
                                               mCourse.getUsers().size()));
		
		// Viewpager initialisieren
		mPager = (ViewPager)findViewById(R.id.teachercoursedetail_viewPager);
		
		if(mPagerAdapter == null) {
			
			mPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {

				@Override
				public Fragment getItem(int position) {
					
					if(position == 0)
						return mPieChart;
					else 
						return mLineChart;
				}

				@Override
				public int getCount() {

					return 2;
				}
			};
		}
		
		if(mPager != null) {
			
			mPager.setAdapter(mPagerAdapter);
			mPager.setPageTransformer(true, new DepthPageTransformer());
			
			// PageListener
			mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

				@Override
				public void onPageSelected(int position) {
					
					if(mActionBar.getSelectedNavigationIndex() != position)
						mActionBar.setSelectedNavigationItem(position);
					
					((TeacherCourseDetailLinechartFragment)mLineChart).setGrabTouch(position == 1);
				}

				@Override
				public void onPageScrollStateChanged(int position) {}

				@Override
				public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
			});
			
			try {
				
				Field scroller = ViewPager.class.getDeclaredField("mScroller");
				scroller.setAccessible(true);
				scroller.set(mPager, new FixedSpeedScroller(this));
			}
			catch (NoSuchFieldException e) {}
			catch (IllegalArgumentException e) {}
			catch (IllegalAccessException e) {}
		}
		
		
		// ActionBar Tabs hinzufügen
		if(mActionBar == null) {
				
			mActionBar = getActionBar();
			mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			
			ActionBar.TabListener tabListener = new ActionBar.TabListener() {
				
				@Override
				public void onTabSelected(ActionBar.Tab actionTab, FragmentTransaction ft) {
					
					if(mPager.getCurrentItem() != actionTab.getPosition())
						mPager.setCurrentItem(actionTab.getPosition());
				}
				
				@Override
				public void onTabReselected(android.app.ActionBar.Tab tab, FragmentTransaction ft) {}
				
				@Override
				public void onTabUnselected(android.app.ActionBar.Tab tab, FragmentTransaction ft) {}
			};
			
			mActionBar.addTab(mActionBar.newTab().setText("Pie Chart").setTabListener(tabListener));
			mActionBar.addTab(mActionBar.newTab().setText("Line Chart").setTabListener(tabListener));
		}
	}
	
	
	
	@Override
	protected void onResume() {
		
		super.onResume();
		
		// Alle Statistikdaten frisch anfordern
		ModuleDAO.invalidateDurationPerCategory();
		ModuleDAO.invalidateDurationPerWeek();
		
		ModuleDAO.beginJob();
		ModuleDAO.getDurationPerCategoryFromServer(DAO_REQUEST_DURATION_PER_CATEGORY, mCourseID);
		ModuleDAO.getDurationPerWeekFromServer(DAO_REQUEST_DURATIONS_PER_WEEK, mCourseID);
		ModuleDAO.commitJob(this, this);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case android.R.id.home:
			finish();
			return true;
		}

		return false;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		
		super.onSaveInstanceState(outState);
	
		getSupportFragmentManager().putFragment(outState,
				                                "linechart",
				                                mLineChart);
		
		getSupportFragmentManager().putFragment(outState,
				                                "piechart",
				                                mPieChart);
	}
	

	
	@Override
	public void onDAOError(int requestID, String message) {
		
		switch(requestID) {
		
		case DAO_REQUEST_DURATION_PER_CATEGORY:
		case DAO_REQUEST_DURATIONS_PER_WEEK:
			Toast.makeText(this,
					       String.format(getString(R.string.loadingerror_statisticsdata),
					    		         message),
					       Toast.LENGTH_LONG).show();
			break;
		}
		
		updateCharts();
	}
	
	@Override
	public void onDAOFinished() {
		
		updateCharts();
	}
	
	private void updateCharts() {
		
		((TeacherCourseDetailPiechartFragment)mPieChart).updateChart();
		((TeacherCourseDetailLinechartFragment)mLineChart).updateChart();
	}
}