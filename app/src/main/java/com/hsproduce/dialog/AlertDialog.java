/**
 * Copyright (C) 2013-2014 EaseMob Technologies. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hsproduce.dialog;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import com.hsproduce.R;
import com.hsproduce.activity.BaseActivity;
import com.hsproduce.activity.LoginActivity;

public class AlertDialog extends BaseActivity {
	private TextView mTextView;
	private Button mButton;
	private int position;
	private ImageView imageView;
	private EditText editText;
	private boolean isEditextShow;
	private boolean toLogin;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.alert_dialog);
		super.onCreate(savedInstanceState);
		initView();
	}

	public void initView() {
		mButton = (Button) findViewById(R.id.btn_cancel);
		imageView = (ImageView) findViewById(R.id.image);
		editText = (EditText) findViewById(R.id.edit);
		//提示内容
		String msg = getIntent().getStringExtra("msg");
		//提示标题
		String title = getIntent().getStringExtra("title");
		//确认后是否回到登录界面
		toLogin = getIntent().getBooleanExtra("toLogin", false);
		position = getIntent().getIntExtra("position", -1);
		//是否显示取消标题
		boolean isCanceTitle=getIntent().getBooleanExtra("titleIsCancel", false);
		//是否显示取消按钮
		boolean isCanceShow = getIntent().getBooleanExtra("cancel", false);
		//是否显示文本编辑框
		isEditextShow = getIntent().getBooleanExtra("editTextShow",false);
		//转发复制的图片的path
		String path = getIntent().getStringExtra("forwardImage");
		//
		String edit_text = getIntent().getStringExtra("edit_text");

		if(msg != null)
		    ((TextView)findViewById(R.id.alert_message)).setText(msg);
		if(title != null)
			mTextView.setText(title);
		if(isCanceTitle){
			mTextView.setVisibility(View.GONE);
		}
		if(isCanceShow)
			mButton.setVisibility(View.VISIBLE);
		if(path != null){


		}
		if(isEditextShow){
			editText.setVisibility(View.VISIBLE);
			editText.setText(edit_text);
		}
	}

	public void ok(View view){
		setResult(RESULT_OK,new Intent().putExtra("position", position).
				putExtra("ok", "ok"));
		if(toLogin){
			startActivity(new Intent(this, LoginActivity.class));
		}
		finish();
		
	}
	
	public void cancel(View view){
		finish();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event){
		finish();
		return true;
	}
}
