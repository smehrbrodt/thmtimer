package de.thm.mni.thmtimer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import de.thm.mni.thmtimer.model.CourseModel;
import de.thm.mni.thmtimer.util.AbstractAsyncFragment;
import de.thm.mni.thmtimer.util.ModuleDAO;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SearchView.OnQueryTextListener;


public class ModuleSearchFragment extends AbstractAsyncFragment {

	private final int DAO_REQUEST_ALL_COURSES = 0;
	
	private List<CourseModel> mCourseList;
	private List<CourseModel> mAdapterData;
	private SearchView mSearch;
	private ModuleListAdapter mAdapter;
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		
		
		if(mCourseList == null)
			mCourseList = new ArrayList<CourseModel>();
		
		if(mAdapterData == null)
			mAdapterData = new ArrayList<CourseModel>();
		
		if(mAdapter == null)
			mAdapter = new ModuleListAdapter(savedInstanceState);
		
		
		ModuleDAO.setJobSize(1);
		ModuleDAO.loadAllCourseListFromServer(this, DAO_REQUEST_ALL_COURSES);
	}
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		RelativeLayout view = (RelativeLayout)inflater.inflate(R.layout.modulesearchfragment,
				                                               container,
				                                               false);
		
		mSearch = (SearchView)view.findViewById(R.id.searchfragment);
		mSearch.setFocusable(false);
		mSearch.setFocusableInTouchMode(false);
		mSearch.setQueryHint(getString(R.string.search_hint));
		mSearch.setOnQueryTextListener(new OnQueryTextListener() {

			@Override
			public boolean onQueryTextSubmit(String query) {
				
				mSearch.clearFocus();
				return true;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				
				mAdapter.getFilter().filter(newText);
				return true;
			}
		});
		
		
		ListView lv = (ListView) view.findViewById(R.id.searchlist);
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> aView,
					                View view,
					                int pos,
					                long id) {
				
				Activity a = getActivity();
				if (a instanceof EnterModuleActivity) {
					
					EnterModuleActivity ea = (EnterModuleActivity) a;
					
					ea.closeSearch(mAdapter.getItem(pos).getId());
				}
				
			}
		});
		mAdapter.sort(new Comparator<CourseModel>(){

			@Override
			public int compare(CourseModel lhs, CourseModel rhs) {
				
				return lhs.getName().compareTo(rhs.getName());
			}
			
		});
		lv.setAdapter(mAdapter);
		return view;
	}
	

	
	@Override
	public void onDAOError(int requestID, String message) {
		
		switch(requestID) {
		
		case DAO_REQUEST_ALL_COURSES:
			Toast.makeText(getActivity(),
					       String.format("Kann die Kursliste nicht laden: %s", message),
					       Toast.LENGTH_LONG).show();
			break;
		}		
	}

	@Override
	public void onDAOSuccess(int requestID) {
		
		switch(requestID) {
		
		case DAO_REQUEST_ALL_COURSES:
			mCourseList.clear();
			mCourseList.addAll(ModuleDAO.getAllCourseList());
			
			mAdapterData.clear();
			mAdapterData.addAll(mCourseList);
			break;
		}
	}

	@Override
	public void onDAOFinished() {
		
		mAdapter.notifyDataSetChanged();
	}
	
	
	
	public void clearFilter() {
		
		mAdapter.getFilter().filter("");
	}
	
	
	
	private class ModuleListAdapter extends ArrayAdapter<CourseModel> {

		private Bundle mBundle;
		
		public ModuleListAdapter(Bundle bundle) {
			
			super(getActivity(), R.layout.modulelistitem, mAdapterData);
			mBundle = bundle;
		}
		

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			if (convertView == null) {
				
				convertView = getLayoutInflater(mBundle).inflate(R.layout.modulelistitem,
						                                         parent,
						                                         false);
			}
			
			TextView name = (TextView)convertView.findViewById(R.id.moduleName);
			TextView subtext = (TextView)convertView.findViewById(R.id.subtext);
			
			
			final CourseModel course = getItem(position);			
			
			// TODO
			subtext.setText(course.getLecturer().get(0).getLastName());
			name.setText(course.getName());
			
			return convertView;
		}

		@Override
		public Filter getFilter() {
			
			return new Filter() {

				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					
					constraint = constraint.toString().toLowerCase();
					FilterResults result = new FilterResults();
					
					if(constraint != null && constraint.toString().length() > 0) {
						
						List<CourseModel> found = new ArrayList<CourseModel>();
						for(CourseModel c : mCourseList) {
							
							// TODO
							if(c.getName().toLowerCase().contains(constraint)) {
								
								found.add(c);
							}
						}

						result.values = found;
						result.count = found.size();
					}
					else {
						
						result.values = mCourseList;
						result.count = mCourseList.size();
					}
					
					return result;
				}

				@SuppressWarnings("unchecked")
				@Override
				protected void publishResults(CharSequence constraint, FilterResults results) {
					
					List<CourseModel> res = (List<CourseModel>)results.values;
					
					clear();
					for(CourseModel item : res)
						add(item);
					
					notifyDataSetChanged();
				}
			};
		}
	}
}