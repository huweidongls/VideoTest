/*  
 * 项目名: YYS 
 * 文件名: MediaPlayOnlineFragment.java  
 * 版权声明:
 *      本系统的所有内容，包括源码、页面设计，文字、图像以及其他任何信息，
 *      如未经特殊说明，其版权均属大华技术股份有限公司所有。
 *      Copyright (c) 2015 大华技术股份有限公司
 *      版权所有
 */
package com.jingna.videotest.lechange.mediaplay.fragment;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.jingna.videotest.R;
import com.jingna.videotest.lechange.business.Business;
import com.jingna.videotest.lechange.business.entity.ChannelInfo;
import com.jingna.videotest.lechange.business.entity.ChannelPTZInfo;
import com.jingna.videotest.lechange.business.util.MediaPlayHelper;
import com.jingna.videotest.lechange.common.ProgressDialog;
import com.lechange.common.log.Logger;
import com.lechange.opensdk.listener.LCOpenSDK_EventListener;
import com.lechange.opensdk.listener.LCOpenSDK_TalkerListener;
import com.lechange.opensdk.media.LCOpenSDK_Talk;

import java.io.FileOutputStream;

/**
 * 描述：实时视频监控 作者： lc
 */
public class MediaPlayOnlineFragment extends MediaPlayFragment implements
		OnClickListener {

	private static final String TAG = "LCopen_MediaPlayOnlineFragment";

	protected static final int MediaMain = 0; // 主码流
	protected static final int MediaAssist = 1; // 辅码流
	protected static final int RECORDER_TYPE_DAV = 3; // 录制格式DAV
	protected static final int RECORDER_TYPE_MP4 = 4; // 录制格式MP4

	public enum Cloud {
		up, down, left, right, leftUp, rightUp, leftDown, RightDown, zoomin, zoomout, stop
	}

	// 状态值
	private int bateMode = MediaMain;

	private enum AudioTalkStatus {
		talk_close, talk_opening, talk_open
	}

	private AudioTalkStatus mOpenTalk = AudioTalkStatus.talk_close; // 语音对讲状态
	private boolean isRecord = false; // 正在录制
	private boolean isOpenSound = false; // 声音打开
	private boolean isPlaying; // 正在播放
	private boolean IsPTZOpen = false; // 云台打开

	private ChannelInfo channelInfo;

	private LinearLayout mLiveMenu;
	private ImageView mLiveMode;//SD/HD
	private ImageView mLivePtz;//云台控制
	private ImageView mLiveSound;//声音
	private ImageView mLiveScale;//全屏

	private LinearLayout mLiveUseLayout;
	private ImageView mLiveScreenshot; //截屏
	private ImageView mLiveTalk; //对讲
	private ImageView mLiveRecord; //录屏

	/**
	 * @see com.dahua.hsviewclientopendemo.mediaplay.MediaPlayFragment#onCreate(android.os.Bundle)
	 *      描述：
	 * @param savedInstanceState
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle b = getArguments();
		if (b != null) {
			String channelId = b.getString("RESID");
			channelInfo = Business.getInstance().getChannel(channelId);
		}
		if (channelInfo == null) {
			Logger.e(TAG, " **server returned channelInfo == null, leads to toast: Device NOT FOUND");
			toast(getResources().getString(R.string.toast_device_not_exist));
			getActivity().setResult(-1);
			getActivity().finish();
		}
	}

	/**
	 * @see com.dahua.hsviewclientopendemo.mediaplay.MediaPlayFragment#onCreateView(android.view.LayoutInflater,
	 *      android.view.ViewGroup, android.os.Bundle) 描述：
	 * @return
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View mView = inflater.inflate(R.layout.fragment_media_live, container,
				false);
		// 必须赋值，父类需要使用到
		mSurfaceParentView = (ViewGroup) mView.findViewById(R.id.live_window);
		// 初始化窗口大小
		LayoutParams mLayoutParams = (LayoutParams) mSurfaceParentView
				.getLayoutParams();
		DisplayMetrics metric = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(metric);
		mLayoutParams.width = metric.widthPixels; // 屏幕宽度（像素）
		mLayoutParams.height = metric.widthPixels * 9 / 16;
		mLayoutParams.setMargins(0, 10, 0, 0);
		mSurfaceParentView.setLayoutParams(mLayoutParams);

		mPlayWin.initPlayWindow(this.getActivity(),
				(ViewGroup) mView.findViewById(R.id.live_window_content), 0);

		mProgressDialog = (ProgressDialog) mView
				.findViewById(R.id.live_play_load);
		mReplayTip = (TextView) mView.findViewById(R.id.live_play_pressed);

		mLiveMenu = (LinearLayout) mView.findViewById(R.id.live_menu);
		mLiveMode = (ImageView) mView.findViewById(R.id.live_mode);
		mLivePtz = (ImageView) mView.findViewById(R.id.live_ptz);
		mLiveSound = (ImageView) mView.findViewById(R.id.live_sound);
		mLiveUseLayout = (LinearLayout) mView
				.findViewById(R.id.live_use_layout);
		mLiveScale = (ImageView) mView.findViewById(R.id.live_scale);
		mLiveScreenshot = (ImageView) mView.findViewById(R.id.live_screenshot);
		mLiveTalk = (ImageView) mView.findViewById(R.id.live_talk);
		mLiveRecord = (ImageView) mView.findViewById(R.id.live_record);

		mReplayTip.setOnClickListener(this);
		mLiveMode.setOnClickListener(this);
		mLivePtz.setOnClickListener(this);
		mLiveSound.setOnClickListener(this);
		mLiveUseLayout.setOnClickListener(this);
		mLiveScale.setOnClickListener(this);
		mLiveScreenshot.setOnClickListener(this);
		mLiveTalk.setOnClickListener(this);
		mLiveRecord.setOnClickListener(this);

		return mView;

	}

	/**
	 * @see com.dahua.hsviewclientopendemo.mediaplay.MediaPlayFragment#onActivityCreated(android.os.Bundle)
	 *      描述：MediaPlayActivity创建完毕
	 * @param savedInstanceState
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		listener = new MyBaseWindowListener();
		mPlayWin.setWindowListener(listener);//为播放窗口设置监听
		mPlayWin.openTouchListener();//开启收拾监听

		// 开启横竖屏切换
		startListener();
		//开启原因：无对讲能力及设备给出提示
		if (channelInfo != null) {
			int ability = channelInfo.getAbility();
			Logger.d(TAG, "==ability== : " + channelInfo.getAbility() + ", isEnable==" + (ability & ChannelInfo.Ability.AudioTalk));
			if ((ability & ChannelInfo.Ability.AudioTalk) == 0) {
				if (getActivity().getResources().getConfiguration().orientation ==
						Configuration.ORIENTATION_LANDSCAPE) {
					mLiveTalk.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							toast(R.string.video_monitor_unsupport_audio_talk);
						}
					});
				} else if
						(getActivity().getResources().getConfiguration().orientation ==
								Configuration.ORIENTATION_PORTRAIT) {
					mLiveTalk.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							toast(R.string.video_monitor_unsupport_audio_talk);
						}
					});
				}
			}
		}
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		play(0);
	}

	@Override
	public void onPause() {
		super.onPause();
		// 停止实时视频,无效资源号
		stop(-1);
		// 关闭语音对讲
		if (mOpenTalk == AudioTalkStatus.talk_open) {
			stopTalk();
		}
	}

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
		mPlayWin.uninitPlayWindow();// 销毁底层资源
	}

	/**
	 * @see com.dahua.hsviewclientopendemo.mediaplay.MediaPlayFragment#resetViews(android.content.res.Configuration)
	 *      描述：实现个性化界面
	 * @param mConfiguration
	 */
	@Override
	protected void resetViews(Configuration mConfiguration) {
		super.resetViews(mConfiguration);
		if (mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) { // 横屏
			mLiveUseLayout.setVisibility(View.GONE);
			mLiveScale.setTag("LANDSCAPE");
			mLiveScale.setImageResource(R.drawable.live_btn_smallscreen);
		} else if (mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
			mLiveUseLayout.setVisibility(View.VISIBLE);
			mLiveScale.setTag("PORTRAIT");
			mLiveScale.setImageResource(R.drawable.live_btn_fullscreen);
		}
	}

	class MyBaseWindowListener extends LCOpenSDK_EventListener {
		@Override
		public void onPlayerResult(int index, String code, int type) {
            Logger.d(TAG, "index : " + index + "  code : " + code + " type : " + type);
            if (type == Business.RESULT_SOURCE_OPENAPI) {
                if (mHander != null) {
                    mHander.post(new Runnable() {
                        public void run() {
                            if (isAdded()) {
                                stop(R.string.video_monitor_play_error);
                            }
                        }
                    });
                }
            } else {
				if (type == Business.RESULT_SOURCE_TYPE_DHHTTP) {
					if (!(code.equals(Business.PlayerResultCode.DHHTTPCode.STATE_DHHTTP_OK) || code.equals(Business.PlayerResultCode.DHHTTPCode.STATE_MSG_HTTPDH_PASSWORD_SALT))) {
						if (mHander != null) {
							mHander.post(new Runnable() {
								public void run() {
									if (isAdded()) {
										stop(R.string.video_monitor_play_error);
									}
								}
							});
						}
					}
				} else if (type == Business.RESULT_SOURCE_TYPE_RTSP) {
					if (code.equals(Business.PlayerResultCode.STATE_PACKET_FRAME_ERROR)
							|| code.equals(Business.PlayerResultCode.STATE_RTSP_TEARDOWN_ERROR)
							|| code.equals(Business.PlayerResultCode.STATE_RTSP_AUTHORIZATION_FAIL)
							|| code.equals(Business.PlayerResultCode.STATE_RTSP_KEY_MISMATCH)) {
						if (mHander != null) {
							mHander.post(new Runnable() {
								public void run() {
									if (isAdded()) {
										stop(R.string.video_monitor_play_error);
									}
								}
							});
						}
					}
				}

			}

        }

		@Override
		public void onPlayBegan(int index) {
			// TODO Auto-generated method stub
			// 显示码率
			// if (mHander != null) {
			// mHander.post(MediaPlayOnlineFragment.this);
			// }
            Logger.d(TAG, "index : " + index + "onPlayBegan --------");
			isPlaying = true;
			// 建立码流,自动开启音频
			if (mHander != null) {
				mHander.post(new Runnable() {
					@Override
					public void run() {
						if (isAdded()) {
							// showLoading(R.string.video_monitor_data_cache);
							onClick(mLiveSound);
						}
					}
				});
			}
			// 关闭播放加载控件
			mProgressDialog.setStop();
		}

		// public void onReceiveData(int len) {
		// // 流量统计
		// mTotalFlow += len;
		// }
		@Override
		public void onStreamCallback(int index, byte[] data, int len) {
			// Log.d(TAG, "LCOpenSDK_EventListener::onStreamCallback-size : " +
			// len);
			try {
				String path = Environment.getExternalStorageDirectory()
						.getPath() + "/streamCallback.ts";
				FileOutputStream fout = new FileOutputStream(path, true);
				fout.write(data);
				fout.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		@Override
		public void onWindowLongPressBegin(int index, Direction dir, float dx,
				float dy) {
			sendCloudOrder(direction2Cloud(dir), true);
		}

		@Override
		public void onWindowLongPressEnd(int index) {
			sendCloudOrder(Cloud.stop, false);
		}

		// 电子缩放
		@Override
		public void onZooming(int index, float dScale) {
			if (IsPTZOpen == false) {
				mPlayWin.doScale(dScale);
			}

		}

		// 云台缩放
		@Override
		public void onZoomEnd(int index, ZoomType zoom) {
			Logger.d(TAG, "onZoomEnd" + zoom);
			if (IsPTZOpen == false) {
				return;
			}
			// TODO Auto-generated method stub
			sendCloudOrder(zoom == ZoomType.ZOOM_IN ? Cloud.zoomin
					: Cloud.zoomout, false);
		}

		// 滑动开始
		@Override
		public boolean onSlipBegin(int index, Direction dir, float dx, float dy) {
			if (IsPTZOpen == false && mPlayWin.getScale() > 1) {
				Logger.d(TAG, "onflingBegin ");
			}
			sendCloudOrder(direction2Cloud(dir), false);
			return true;
		}

		// 滑动中
		@Override
		public void onSlipping(int index, Direction dir, float prex,
				float prey, float dx, float dy) {
			if (IsPTZOpen == false && mPlayWin.getScale() > 1) {
				Logger.d(TAG, "onflingBegin onFlinging");
				mPlayWin.doTranslate(dx, dy);
			}
		}

		// 滑动结束
		@Override
		public void onSlipEnd(int index, Direction dir, float dx, float dy) {
			if (IsPTZOpen == false && mPlayWin.getScale() > 1) {
				Logger.d(TAG, "onflingBegin onFlingEnd");
				return;
			}

			// sendCloudOrder(Cloud.stop, false);
		}

		private Cloud direction2Cloud(Direction dir) {
			Cloud cloud = null;
			switch (dir) {
			case Up:
				cloud = Cloud.up;
				break;
			case Down:
				cloud = Cloud.down;
				break;
			case Left:
				cloud = Cloud.left;
				break;
			case Right:
				cloud = Cloud.right;
				break;
			case Left_up:
				cloud = Cloud.leftUp;
				break;
			case Left_down:
				cloud = Cloud.leftDown;
				break;
			case Right_up:
				cloud = Cloud.rightUp;
				break;
			case Right_down:
				cloud = Cloud.RightDown;
				break;
			default:
				break;
			}
			return cloud;
		}

		public void onWindowDBClick(int index, float dx, float dy) {
			// TODO Auto-generated method stub
			switch (mLiveMenu.getVisibility()) {
			case View.GONE:
				mLiveMenu.setVisibility(View.VISIBLE);
				break;
			case View.VISIBLE:
				mLiveMenu.setVisibility(View.GONE);
				break;
			default:
				break;
			}
		}

	}

	private void sendCloudOrder(final Cloud mCloud, final boolean longclick) {
		if (IsPTZOpen && isPlaying) {
			// super.sendCloudOrder(mCloud, longclick);
			Logger.d(TAG, "-----is longclick:" + longclick);
			ChannelPTZInfo.Operation operation = ChannelPTZInfo.Operation.Move;
			ChannelPTZInfo.Direction direction = null;
			switch (mCloud) {
			case up:
				direction = ChannelPTZInfo.Direction.Up;
				Logger.d(TAG, "-----Up");
				break;
			case down:
				direction = ChannelPTZInfo.Direction.Down;
				Logger.d(TAG, "-----Down");
				break;
			case left:// 手势向左， 摄像头向右转
				direction = ChannelPTZInfo.Direction.Left;
				Logger.d(TAG, "-----case left");
				break;
			case right:
				direction = ChannelPTZInfo.Direction.Right;
				Logger.d(TAG, "-----case right");
				break;
			case zoomin:
				direction = ChannelPTZInfo.Direction.ZoomIn;
				Logger.d(TAG, "-----ZoomIn");
				break;
			case zoomout:
				direction = ChannelPTZInfo.Direction.ZoomOut;
				Logger.d(TAG, "-----ZoomOut");
				break;
			case stop:
				Logger.d(TAG, "-----stop");
				operation = ChannelPTZInfo.Operation.Stop;
				// direction = ChannelPTZInfo.Direction.Down;
				break;
			default:
				break;
			}
			ChannelPTZInfo ptzInfo = new ChannelPTZInfo(operation, direction);
			ptzInfo.setDuration(ChannelPTZInfo.Duration.Short);
			if (longclick) {
				ptzInfo.setDuration(ChannelPTZInfo.Duration.Long);
			}

			Business.getInstance().AsynControlPtz(channelInfo.getUuid(),
					ptzInfo, new Handler() {
						@Override
						public void handleMessage(Message msg) {
							if (!isAdded() || getActivity() == null) {
								Logger.d(TAG, "*******page not exits");
								return;
							}
							// Logger.d(TAG,
							// "-----control callback what:"+msg.what);
							Business.RetObject retObject = (Business.RetObject) msg.obj;
							if (msg.what == 0) {
								Logger.d(TAG, "-----control result: true");
							} else
								toast(retObject.mMsg);
						}
					});
		}
	}

	/**
	 * 描述：开始播放
	 */
	public void play(int strRes) {
		// if (isPlaying) {
        Logger.e(TAG, "startplay: --------------");
		stop(-1);
		// }
		if (strRes > 0) {
			showLoading(strRes);
		} else {
			showLoading(R.string.common_loading);
		}
		mPlayWin.playRtspReal(
				Business.getInstance().getToken(),
				channelInfo.getDeviceCode(),
				channelInfo.getEncryptKey() != null ? channelInfo
						.getEncryptKey() : channelInfo.getDeviceCode(),
				channelInfo.getIndex(), bateMode, true);//最后一个参数为true，表示使用长连接
	}

	/**
	 * 描述：停止播放
	 */
	public void stop(final int res) {
		// 关闭播放加载控件
		mProgressDialog.setStop();

		if (isRecord) {
			stopRecord();// 关闭录像
		}

		if (isOpenSound) {
			closeAudio();// 关闭音频
			isOpenSound = false;
			mLiveSound.setImageResource(R.drawable.live_btn_sound_off);
		}
		mPlayWin.stopRtspReal();// 关闭视频
		isPlaying = false;

		if (mHander != null) {
			mHander.post(new Runnable() {
				public void run() {
					if (isAdded()) {
						if (res > 0)
							showErrorTip(res);
					}
				}
			});
		}
	}

	/**
	 * 描述：抓拍图像
	 */
	public String capture() {
		String captureFilePath = null;
		// 判断SD卡是否已存在
		// SD卡容量检查
		// FIXME 检查设备是否在线
		// 抓图
		String channelName = null;
		if (channelInfo != null) {
			channelName = channelInfo.getName();
		} else {
			channelName = "";
		}

		// 去除通道中在目录中的非法字符
		channelName = channelName.replace("-", "");

		captureFilePath = MediaPlayHelper.getCaptureAndVideoPath(
				MediaPlayHelper.DHFilesType.DHImage, channelName);
		int ret = mPlayWin.snapShot(captureFilePath);
		if (ret == retOK) {
			// 扫描到相册中
			MediaScannerConnection.scanFile(getActivity(),
					new String[] { captureFilePath }, null, null);
			Toast.makeText(getActivity(),
					R.string.video_monitor_image_capture_success,
					Toast.LENGTH_SHORT).show();
		} else {
			captureFilePath = null;
			Toast.makeText(getActivity(),
					R.string.video_monitor_image_capture_failed,
					Toast.LENGTH_SHORT).show();
		}
		return captureFilePath;
	}

	/**
	 * 描述：开始录制
	 */
	String path = null;

	public boolean startRecord() {
		if (!isPlaying) {
			toast(R.string.video_monitor_video_record_failed);
			return false;
		}
		// 判断SD卡是否已存在
		// SD卡容量检查

		// FIXME 要做 ~~~~检查设备是否在线

		// 录像的路径
		String channelName = null;
		if (channelInfo != null) {
			channelName = channelInfo.getName();
		} else {
			channelName = "";
		}

		// 去除通道中在目录中的非法字符
		channelName = channelName.replace("-", "");

		path = MediaPlayHelper.getCaptureAndVideoPath(MediaPlayHelper.DHFilesType.DHVideo,
				channelName);
		int recordType = 1;
		long spaceRemain = 0x7FFFFFFF; //保留字段
		// 开始录制 1
		int ret = mPlayWin.startRecord(path, recordType, spaceRemain);
		if (ret != retOK) {
			toast(R.string.video_monitor_video_record_failed);
			return false;
		}

		return true;
	}

	/**
	 * 关闭录像
	 */
	public void stopRecord() {
		mPlayWin.stopRecord();
		isRecord = false;
		toast(getString(R.string.video_monitor_video_record_success));
		mLiveRecord.setImageResource(R.drawable.live_btn_record_nor);

		MediaScannerConnection.scanFile(getActivity(), new String[] { path },
				null, null);
	}

	/**
	 * 打开声音
	 */
	public boolean openAudio() {
		return mPlayWin.playAudio() == retOK;
	}

	/**
	 * 关闭声音
	 */
	public boolean closeAudio() {
		return mPlayWin.stopAudio() == retOK;
	}

	/**
	 * 描述：开始对讲
	 */
	public void startTalk() {
		if (!isPlaying) {
			toast(R.string.video_monitor_talk_open_error);
			return;
		}

		toastWithImg(getString(R.string.video_monitor_media_talk),
				R.drawable.live_pic_talkback);

		// 替换图片
		mLiveTalk.setImageResource(R.drawable.live_btn_talk_click);
		mOpenTalk = AudioTalkStatus.talk_opening;
		// 关闭扬声器 默认为关
		if (isOpenSound) {
			closeAudio();
			mLiveSound.setImageResource(R.drawable.live_btn_sound_off);
		}
		mLiveSound.setClickable(false);
		LCOpenSDK_Talk.setListener(new AudioTalkerListener());//对讲前先设备监听
		LCOpenSDK_Talk.playTalk(
				Business.getInstance().getToken(),
				channelInfo.getDeviceCode(),
				channelInfo.getEncryptKey() != null ? channelInfo
						.getEncryptKey() : channelInfo.getDeviceCode(), true);//对讲使用长连接
	}

	/**
	 * 描述：停止对讲
	 */
	public void stopTalk() {
		toast(R.string.video_monitor_media_talk_close);
		// 替换图片
		mLiveTalk.setImageResource(R.drawable.live_btn_talk_nor);
		LCOpenSDK_Talk.stopTalk();
		// 解决gc回收问题
		LCOpenSDK_Talk.setListener(null);//停止对讲后对讲监听置为空
		mOpenTalk = AudioTalkStatus.talk_close;
		// 开启扬声器
		if (isOpenSound && isPlaying) {
			openAudio();
			mLiveSound.setImageResource(R.drawable.live_btn_sound_on);
		}
		mLiveSound.setClickable(true);
	}

	class AudioTalkerListener extends LCOpenSDK_TalkerListener {
		/**
		 * 描述：对讲状态获取
		 */
		@Override
		public void onTalkResult(String error, int type) {
			// TODO Auto-generated method stub
			if (type == Business.RESULT_SOURCE_OPENAPI
					|| error.equals(AUDIO_TALK_ERROR)
					|| error.equals(Business.PlayerResultCode.STATE_PACKET_FRAME_ERROR)
					|| error.equals(Business.PlayerResultCode.STATE_RTSP_TEARDOWN_ERROR)
					|| error.equals(Business.PlayerResultCode.STATE_RTSP_AUTHORIZATION_FAIL)) {
				if (mHander != null) {
					mHander.post(new Runnable() {
						public void run() {
							if (isAdded()) {
								// 提示对讲打开失败
								toast(R.string.video_monitor_talk_open_error);
								stopTalk();// 关闭播放
							}
						}
					});
				}
			} else if (error.equals(Business.PlayerResultCode.STATE_RTSP_PLAY_READY)) {
				if (mHander != null) {
					mHander.post(new Runnable() {
						public void run() {
							if (isAdded()) {
								// 提示对讲打开成功
								toast(R.string.video_monitor_media_talk_ready);
							}
						}
					});
				}
				mOpenTalk = AudioTalkStatus.talk_open;
			}

		}

		@Override
		public void onTalkPlayReady() {
			// TODO Auto-generated method stub

		}
		
	    @Override
        public void onAudioRecord(byte[] pData, int length, int audioFormat,int sampleRate, int sampleDepth) {
//	            super.onAudioRecord(pData, length, audioFormat, sampleRate, sampleDepth);
//	            BufferedOutputStream bufferedOutputStream;
//	            FileOutputStream fileOutputStream;
//	            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioFile.aac";
//	            File file = new File(filePath);
//	            if (file.exists()) {
//	                try {
//	                    fileOutputStream = new FileOutputStream(file, true);
//	                    bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
//	                    try {
//	                        bufferedOutputStream.write(pData);
//	                        bufferedOutputStream.flush();
//	                        bufferedOutputStream.close();
//	                    } catch (IOException e) {
//	                        e.printStackTrace();
//	                    }
//	                } catch (FileNotFoundException e) {
//	                    e.printStackTrace();
//	                } finally {
//
//	                }
//	            }else{
//	                try {
//	                    file.createNewFile();
//	                } catch (IOException e) {
//	                    e.printStackTrace();
//	                }
//	            }
	        }

	        @Override
	        public void onAudioReceive(byte[] pData, int length, int audioFormat, int sampleRate, int sampleDepth) {
	            super.onAudioReceive(pData, length, audioFormat, sampleRate, sampleDepth);
	        }
	    
	}

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		switch (view.getId()) {
		case R.id.live_ptz:
			if(!isPlaying){
				return;
			}
			if ((channelInfo.getAbility() & ChannelInfo.Ability.PTZ) != 0) {
				if (IsPTZOpen) {
					// 测试专用
					mPlayWin.setStreamCallback(0);
					IsPTZOpen = false;
					mLivePtz.setImageResource(R.drawable.live_btn_ptz_off);
				} else {
					// 测试专用
					mPlayWin.setStreamCallback(1);
					IsPTZOpen = true;
					mLivePtz.setImageResource(R.drawable.live_btn_ptz_on);
				}
			} else {
				toast(R.string.toast_device_ability_no_ptz);
			}
			break;
		case R.id.live_scale://全屏
			if ("LANDSCAPE".equals(mLiveScale.getTag())) {
				mOrientation = ORIENTATION.isPortRait;
				getActivity().setRequestedOrientation(
						ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			} else {
				mOrientation = ORIENTATION.isLandScape;
				getActivity().setRequestedOrientation(
						ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			}
			break;
		case R.id.live_mode://清晰度切换
			// if(isPlaying) //播放是个异步的,多次点击会使停止播放顺序乱掉
			if (mOpenTalk == AudioTalkStatus.talk_opening) {
				stopTalk();
			}
			// 高清切换到流畅
			if (bateMode == MediaMain) {
				bateMode = MediaAssist;
				mLiveMode.setImageResource(R.drawable.live_btn_fluent);
				play(R.string.video_monitor_change_stream_normal);
			}// 流畅切换到高清
			else if (bateMode == MediaAssist) {
				bateMode = MediaMain;
				mLiveMode.setImageResource(R.drawable.live_btn_hd);
				play(R.string.video_monitor_change_stream_hd);
			}
			break;
		case R.id.live_talk://对讲
			switch (mOpenTalk) {
			case talk_open:
			case talk_opening:
				stopTalk();
				break;
			case talk_close:

				openTalk();
//				startTalk();
				break;
			default:
				break;
			}
			break;
		case R.id.live_sound://开启关闭声音
			if (mOpenTalk != AudioTalkStatus.talk_close || !isPlaying) {
				toast(R.string.video_monitor_load_talk_sound_error);
			} else {
				if (isOpenSound) {
					boolean result = closeAudio();
					if (result) {
						mLiveSound
								.setImageResource(R.drawable.live_btn_sound_off);
						toast(R.string.video_monitor_sound_close);
						isOpenSound = false;
					}
				} else {
					boolean result = openAudio();
					if (result) {
						mLiveSound
								.setImageResource(R.drawable.live_btn_sound_on);
						toast(R.string.video_monitor_sound_open);
						isOpenSound = true;
					}
				}
			}
			break;
		case R.id.live_screenshot://截屏
			mLiveScreenshot
					.setImageResource(R.drawable.live_btn_screenshot_click);
			capture();
			mLiveScreenshot
					.setImageResource(R.drawable.live_btn_screenshot_nor);
			break;
		case R.id.live_record://录像
			if (!isRecord) {
				boolean result = startRecord();
				if (result) {
					toastWithImg(
							getString(R.string.video_monitor_media_record),
							R.drawable.live_pic_record);
					isRecord = true;
					mLiveRecord
							.setImageResource(R.drawable.live_btn_record_click);
				}
			} else {
				stopRecord();
			}
			break;
		case R.id.live_play_pressed:
			play(-1);
			break;
		default:
			break;
		}
	}

	/**
	 * 权限检查）
	 */
	public void openTalk() {
		boolean isMinSDKM = Build.VERSION.SDK_INT < 23;
		boolean isGranted = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
		if (isMinSDKM || isGranted) {
			startTalk();
			// 对讲权限
			return;
		}
		requestRecordAudioPermission();
	}

	private void requestRecordAudioPermission() {

		if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
				Manifest.permission.RECORD_AUDIO)) {

			Logger.d("Uriah", "Uriah + shouldShowRequestPermission true");
			ActivityCompat.requestPermissions(getActivity(),
					new String[]{Manifest.permission.RECORD_AUDIO},
					1);
		} else {
			ActivityCompat.requestPermissions(getActivity(),
					new String[]{Manifest.permission.RECORD_AUDIO},
					1);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			startTalk();

		} else {
			if (requestCode == 1) {
				toast(getString(R.string.toast_permission_record_audio_forbidden));
			}

		}
	}
}
