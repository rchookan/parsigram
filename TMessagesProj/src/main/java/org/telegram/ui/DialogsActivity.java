/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.ui;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.query.SearchQuery;
import org.telegram.messenger.query.StickersQuery;
import org.telegram.messenger.support.widget.LinearLayoutManager;
import org.telegram.messenger.support.widget.RecyclerView;
import org.telegram.messenger.FileLog;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Adapters.DialogsAdapter;
import org.telegram.ui.Adapters.DialogsSearchAdapter;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Cells.DrawerActionCell;
import org.telegram.ui.Cells.DrawerProfileCell;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.HashtagSearchCell;
import org.telegram.ui.Cells.HintDialogCell;
import org.telegram.ui.Cells.LoadingCell;
import org.telegram.ui.Cells.ProfileSearchCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Cells.DialogCell;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.MenuDrawable;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.Favourite;
import org.telegram.ui.Components.FragmentContextView;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadialProgressView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.Calendar;

public class DialogsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;
    private DialogsAdapter dialogsAdapter;
    private DialogsSearchAdapter dialogsSearchAdapter;
    private EmptyTextProgressView searchEmptyView;
    private RadialProgressView progressView;
    private LinearLayout emptyView;
    private ActionBarMenuItem passcodeItem;
    private ImageView floatingButton;
    private RecyclerView sideMenu;
    private FragmentContextView fragmentContextView;

    private TextView emptyTextView1;
    private TextView emptyTextView2;

    private AlertDialog permissionDialog;

    private int prevPosition;
    private int prevTop;
    private boolean scrollUpdated;
    private boolean floatingHidden;
    private final AccelerateDecelerateInterpolator floatingInterpolator = new AccelerateDecelerateInterpolator();

    private boolean checkPermission = true;

    private String selectAlertString;
    private String selectAlertStringGroup;
    private String addToGroupAlertString;
    private int dialogsType;

    public static boolean dialogsLoaded;
    private boolean searching;
    private boolean searchWas;
    private boolean onlySelect;
    private long selectedDialog;
    private String searchString;
    private long openedDialogId;
    private boolean cantSendToChannels;
    //Teleh
    private LinearLayout tabsLayout;
    private FrameLayout tabsView;
    private ImageView usersTab;
    private ImageView allTab;
    private ImageView groupsTab;
    private ImageView superGroupsTab;
    private ImageView channelsTab;
    private ImageView botsTab;
    private ImageView favsTab;
    private ActionBarMenuItem ghostItem;
    private boolean hideTabs;
    private DisplayMetrics displayMetrics;
    private float downX;
    private float downY;
    private float upX;
    private float upY;
    private float vDPI;
    private float touchPositionDP;
    private int tabsHeight = 40;
    private boolean tabsHidden;
    private boolean disableAnimation;
    private TextView allCounter;
    private TextView botsCounter;
    private TextView channelsCounter;
    private TextView favsCounter;
    private TextView groupsCounter;
    private TextView sGroupsCounter;
    private TextView usersCounter;
    private SharedPreferences preferences;
    private String chatPassword;
    private boolean changedTabPosition;
    private DialogsAdapter dialogsBackupAdapter;
    private int h = AndroidUtilities.dp((float) 40);
    boolean chatUnlocked;

    private DialogsActivityDelegate delegate;

    public interface DialogsActivityDelegate {
        void didSelectDialog(DialogsActivity fragment, long dialog_id, boolean param);
    }

    public DialogsActivity(Bundle args) {
        super(args);
    }

    public void changeGhostModeState() {
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        int iconColor = themePrefs.getInt("chatsHeaderIconsColor", 0xffffffff);

        SharedPreferences.Editor editor = preferences.edit();
        boolean ghostMode;
        if (!preferences.getBoolean("ghost_mode", false)) {
            ghostMode = true;
            Toast.makeText(ApplicationLoader.applicationContext, LocaleController.getString("GhostModeActivated", R.string.GhostModeActivated), Toast.LENGTH_LONG).show();
        } else {
            ghostMode = false;
            Toast.makeText(ApplicationLoader.applicationContext, LocaleController.getString("GhostModeDeactivated", R.string.GhostModeDeactivated), Toast.LENGTH_LONG).show();
        }
        editor.putBoolean("ghost_mode", ghostMode);
        editor.commit();

        actionBar.changeGhostModeVisibility();
        MessagesController.getInstance().reRunUpdateTimerProc();

        if (preferences.getBoolean("show_ghost_state_icon", true) == true) {
            if (ghostItem != null) {
                ActionBarMenuItem actionBarMenuItem = ghostItem;
                if (preferences.getBoolean("ghost_mode", false) == true) {
                    Drawable ic_ghost = getParentActivity().getResources().getDrawable(R.drawable.ic_ghost);
                    if (ic_ghost != null)
                        ic_ghost.setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
                    actionBarMenuItem.setIcon(ic_ghost);
                } else {
                    Drawable ic_ghost_disable = getParentActivity().getResources().getDrawable(R.drawable.ic_ghost_disable);
                    if (ic_ghost_disable != null)
                        ic_ghost_disable.setColorFilter(iconColor, PorterDuff.Mode.MULTIPLY);
                    actionBarMenuItem.setIcon(ic_ghost_disable);
                }
            }
        } else {
            ghostItem.setVisibility(View.GONE);
        }
    }

    private void addTabView(Context context, ImageView iv, TextView textView, boolean show) {
        iv.setScaleType(ImageView.ScaleType.CENTER);
        textView.setGravity(17);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setCornerRadius((float) AndroidUtilities.dp(32.0f));
        textView.setBackgroundDrawable(shape);
        RelativeLayout layout = new RelativeLayout(context);
        layout.addView(iv, LayoutHelper.createRelative(-1, -1));
        layout.addView(textView, LayoutHelper.createRelative(-2, -2, 0, 0, 3, 6, 11));
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) textView.getLayoutParams();
        params.addRule(preferences.getBoolean("tabsToBottom", false) ? 10 : 12);
        textView.setLayoutParams(params);
        if (show) {
            tabsLayout.addView(layout, LayoutHelper.createLinear(0, -1, (float) 1.0f));
        }
    }

    private void unreadCount() {
        unreadCount(MessagesController.getInstance().dialogs, allCounter);
        unreadCount(MessagesController.getInstance().dialogsUsers, usersCounter);
        unreadCount(MessagesController.getInstance().dialogsBots, botsCounter);
        unreadCount(MessagesController.getInstance().dialogsChannels, channelsCounter);
        unreadCount(MessagesController.getInstance().dialogsFavs, favsCounter);
        unreadCountGroups();
    }

    private void unreadCountGroups() {
        if (preferences.getBoolean("hideSGroups", false)) {
            unreadCount(MessagesController.getInstance().dialogsGroupsAll, groupsCounter);
        } else {
            unreadCount(MessagesController.getInstance().dialogsGroups, groupsCounter);
            unreadCount(MessagesController.getInstance().dialogsMegaGroups, sGroupsCounter);
        }
        changedTabPosition = false;
    }

    private void unreadCount(ArrayList<TLRPC.TL_dialog> dialogs, TextView tv) {
        if (!preferences.getBoolean("hideTabs", false)) {
            if (preferences.getBoolean("hideTabsCounters", false)) {
                tv.setVisibility(View.GONE);
                return;
            }
            boolean allMuted = true;
            boolean countDialogs = preferences.getBoolean("tabsCountersCountChats", false);
            boolean countNotMuted = preferences.getBoolean("tabsCountersCountNotMuted", false);
            int unreadCount = 0;
            if (!(dialogs == null || dialogs.isEmpty())) {
                for (int a = 0; a < dialogs.size(); a++) {
                    TLRPC.TL_dialog dialg = dialogs.get(a);
                    boolean isMuted = MessagesController.getInstance().isDialogMuted(dialg.id);
                    if (!isMuted || !countNotMuted) {
                        int i = dialg.unread_count;
                        if (i == 0) {
                            if (preferences.getInt("unread_" + dialg.id, 0) == 1) {
                                i = 1;
                            }
                        }
                        if (i > 0) {
                            if (!countDialogs) {
                                unreadCount += i;
                            } else if (i > 0) {
                                unreadCount++;
                            }
                            if (i > 0 && !isMuted) {
                                allMuted = false;
                            }
                        }
                    }
                }
            }
            if (unreadCount == 0) {
                tv.setVisibility(View.GONE);
            } else {
                tv.setVisibility(View.VISIBLE);
                tv.setText("" + unreadCount);
                SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, 0);
                int size = themePrefs.getInt("chatsHeaderTabCounterSize", 11);
                tv.setTextSize(1, (float) size);
                tv.setPadding(AndroidUtilities.dp(size > 10 ? (float) (size - 7) : 4.0f), 0, AndroidUtilities.dp(size > 10 ? (float) (size - 7) : 4.0f), 0);
                int cColor = themePrefs.getInt("chatsHeaderTabCounterColor", -1);
                if (allMuted) {
                    tv.getBackground().setColorFilter(themePrefs.getInt("chatsHeaderTabCounterSilentBGColor", -4605511), PorterDuff.Mode.SRC_IN);
                    tv.setTextColor(cColor);
                } else {
                    tv.getBackground().setColorFilter(themePrefs.getInt("chatsHeaderTabCounterBGColor", -2937041), PorterDuff.Mode.SRC_IN);
                    tv.setTextColor(cColor);
                }
            }
            if (preferences.getBoolean("tabsToBottom", false)) {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) tv.getLayoutParams();
                params.addRule(preferences.getBoolean("tabsToBottom", false) ? 12 : 10, 0);
                params.addRule(preferences.getBoolean("tabsToBottom", false) ? 10 : 12);
                tv.setLayoutParams(params);
            }
        }
    }

    public void initTabs(Context context) {
        SharedPreferences.Editor editor = preferences.edit();
        boolean hideUsers = preferences.getBoolean("hideUsers", false);
        boolean hideGroups = preferences.getBoolean("hideGroups", false);
        boolean hideSGroups = preferences.getBoolean("hideSGroups", false);
        boolean hideChannels = preferences.getBoolean("hideChannels", false);
        boolean hideBots = preferences.getBoolean("hideBots", false);
        boolean hideFavs = preferences.getBoolean("hideFavs", false);
        hideTabs = preferences.getBoolean("hideTabs", false);
        disableAnimation = preferences.getBoolean("disableTabsAnimation", false);
        if (hideUsers && hideGroups && hideSGroups && hideChannels && hideBots && hideFavs && !hideTabs) {
            hideTabs = true;
            editor.putBoolean("hideTabs", true).apply();
        } else if (!hideUsers || !hideGroups || !hideSGroups || !hideChannels || !hideBots || !hideFavs) {
            hideTabs = false;
            editor.putBoolean("hideTabs", false).apply();
        }
        tabsHeight = preferences.getInt("tabsHeight", 40);
        refreshTabAndListViews(false);
        int selectedTab = preferences.getInt("defTab", -1);
        if (selectedTab == -1) {
            selectedTab = preferences.getInt("selTab", 0);
        }
        if (!(hideTabs || dialogsType == selectedTab)) {
            int i = (selectedTab == 3 && hideSGroups) ? 9 : selectedTab;
            dialogsType = i;
            dialogsAdapter = new DialogsAdapter(context, dialogsType);
            listView.setAdapter(dialogsAdapter);
            dialogsAdapter.notifyDataSetChanged();
        }
        dialogsBackupAdapter = new DialogsAdapter(context, 0);
        tabsLayout = new LinearLayout(context);
        tabsLayout.setOrientation(LinearLayout.HORIZONTAL);
        tabsLayout.setGravity(17);
        allTab = new ImageView(context);
        allTab.setImageResource(R.drawable.tab_all);
        allCounter = new TextView(context);
        allCounter.setTag("ALL");
        addTabView(context, allTab, allCounter, true);
        usersTab = new ImageView(context);
        usersTab.setImageResource(R.drawable.tab_user);
        usersCounter = new TextView(context);
        usersCounter.setTag("USERS");
        addTabView(context, usersTab, usersCounter, !hideUsers);
        groupsTab = new ImageView(context);
        groupsTab.setImageResource(R.drawable.tab_group);
        groupsCounter = new TextView(context);
        groupsCounter.setTag("GROUPS");
        addTabView(context, groupsTab, groupsCounter, !hideGroups);
        superGroupsTab = new ImageView(context);
        superGroupsTab.setImageResource(R.drawable.tab_supergroup);
        sGroupsCounter = new TextView(context);
        sGroupsCounter.setTag("SGROUP");
        addTabView(context, superGroupsTab, sGroupsCounter, !hideSGroups);
        channelsTab = new ImageView(context);
        channelsTab.setImageResource(R.drawable.tab_channel);
        channelsCounter = new TextView(context);
        channelsCounter.setTag("CHANNELS");
        addTabView(context, channelsTab, channelsCounter, !hideChannels);
        botsTab = new ImageView(context);
        botsTab.setImageResource(R.drawable.tab_bot);
        botsCounter = new TextView(context);
        botsCounter.setTag("BOTS");
        addTabView(context, botsTab, botsCounter, !hideBots);
        favsTab = new ImageView(context);
        favsTab.setImageResource(R.drawable.tab_favs);
        favsCounter = new TextView(context);
        favsCounter.setTag("FAVS");
        addTabView(context, favsTab, favsCounter, !hideFavs);
        tabsView.addView(tabsLayout, LayoutHelper.createFrame(-1, GroundOverlayOptions.NO_DIMENSION));
        groupsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialogsType != 4) {
                    dialogsType = 4;
                    refreshAdapter(getParentActivity());
                }
            }
        });
        usersTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialogsType != 6) {
                    dialogsType = 6;
                    refreshAdapter(getParentActivity());
                }
            }
        });
        superGroupsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialogsType != 7) {
                    dialogsType = 7;
                    refreshAdapter(getParentActivity());
                }
            }
        });
        botsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialogsType != 5) {
                    dialogsType = 5;
                    refreshAdapter(getParentActivity());
                }
            }
        });
        channelsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialogsType != 3) {
                    dialogsType = 3;
                    refreshAdapter(getParentActivity());
                }
            }
        });
        allTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialogsType != 0) {
                    dialogsType = 0;
                    refreshAdapter(getParentActivity());
                }

            }
        });
        favsTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialogsType != 8) {
                    dialogsType = 8;
                    refreshAdapter(getParentActivity());
                }

            }
        });
        allTab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("All", R.string.All));
                CharSequence items[];
                SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                final int tabVal = 0;
                final int def = plusPreferences.getInt("defTab", -1);
                final int sort = plusPreferences.getInt("sortAll", 0);

                CharSequence cs2 = def == tabVal ? LocaleController.getString("ResetDefaultTab", R.string.ResetDefaultTab) : LocaleController.getString("SetAsDefaultTab", R.string.SetAsDefaultTab);
                // CharSequence cs1 = sort == 0 ? LocaleController.getString("SortByUnreadCount", R.string.SortByUnreadCount) : LocaleController.getString("SortByLastMessage", R.string.SortByLastMessage);
                CharSequence cs0 = LocaleController.getString("HideShowTabs", R.string.HideShowTabs);
                items = new CharSequence[]{cs0/*, cs1*/, cs2, LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead)};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = plusPreferences.edit();
                        if (which == 0) {
                            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getParentActivity());
                            createTabsDialog(getParentActivity(), builder);
                            builder.setNegativeButton(LocaleController.getString("Done", R.string.Done), null);
                            showDialog(builder.create());
                        }  else if (which == 1) {
                            editor.putInt("defTab", def == tabVal ? -1 : tabVal).apply();
                        } else if (which == 2) {
                            markAsReadDialog(true);
                        }
                    }


                });
                showDialog(builder.create());
                return true;
            }
        });
        groupsTab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("Groups", R.string.Groups));
                CharSequence items[];
                SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                final boolean hideSGroups = plusPreferences.getBoolean("hideSGroups", false);
                final int tabVal = 4;
                final int sort = plusPreferences.getInt("sortGroups", 0);
                final int def = plusPreferences.getInt("defTab", -1);

                CharSequence cs2 = def == tabVal ? LocaleController.getString("ResetDefaultTab", R.string.ResetDefaultTab) : LocaleController.getString("SetAsDefaultTab", R.string.SetAsDefaultTab);
                // CharSequence cs1 = sort == 0 ? LocaleController.getString("SortByUnreadCount", R.string.SortByUnreadCount) : LocaleController.getString("SortByLastMessage", R.string.SortByLastMessage);
                items = new CharSequence[]{/*cs1,*/ cs2, LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead)};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override


                    public void onClick(DialogInterface dialog, final int which) {

                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = plusPreferences.edit();
                        /*if (which == 0) {
                            editor.putInt("sortGroups", sort == 0 ? 1 : 0).apply();
                            if (dialogsAdapter.getItemCount() > 1) {
                                dialogsAdapter.notifyDataSetChanged();
                            }
                        } else*/ if (which == 0) {
                            editor.putInt("defTab", def == tabVal ? -1 : tabVal).apply();
                        } else if (which == 1) {
                            markAsReadDialog(true);
                        }
                    }
                });
                showDialog(builder.create());
                return true;
            }
        });
        usersTab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("Users", R.string.Users));
                CharSequence items[];
                SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                final int tabVal = 6;
                final int sort = plusPreferences.getInt("sortUsers", 0);
                final int def = plusPreferences.getInt("defTab", -1);
                CharSequence cs = def == tabVal ? LocaleController.getString("ResetDefaultTab", R.string.ResetDefaultTab) : LocaleController.getString("SetAsDefaultTab", R.string.SetAsDefaultTab);
                items = new CharSequence[]{cs, LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead)};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = plusPreferences.edit();
                        if (which == 0) {
                            editor.putInt("defTab", def == tabVal ? -1 : tabVal).apply();
                        }/* else if (which == 0) {
                            editor.putInt("sortUsers", sort == 0 ? 1 : 0).apply();
                            if (dialogsAdapter.getItemCount() > 1) {
                                dialogsAdapter.notifyDataSetChanged();
                            }
                        }*/ else if (which == 1) {
                            markAsReadDialog(true);
                        }
                    }
                });
                showDialog(builder.create());
                return true;
            }
        });
        superGroupsTab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("SuperGroups", R.string.SuperGroups));
                CharSequence items[];
                SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                final int tabVal = 7;
                final int def = plusPreferences.getInt("defTab", -1);
                final int sort = plusPreferences.getInt("sortSGroups", 0);
                final boolean hideSGroups = plusPreferences.getBoolean("hideSGroups", false);
                CharSequence cs2 = def == tabVal ? LocaleController.getString("ResetDefaultTab", R.string.ResetDefaultTab) : LocaleController.getString("SetAsDefaultTab", R.string.SetAsDefaultTab);
                //CharSequence cs1 = sort == 0 ? LocaleController.getString("SortByUnreadCount", R.string.SortByUnreadCount) : LocaleController.getString("SortByLastMessage", R.string.SortByLastMessage);
                items = new CharSequence[]{ cs2, LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead)};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = plusPreferences.edit();

                        if (which == 0) {
                            editor.putInt("defTab", def == tabVal ? -1 : tabVal).apply();
                        } else if (which == 1) {
                            markAsReadDialog(true);
                        }
                    }
                });
                showDialog(builder.create());
                return true;
            }
        });
        botsTab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("Bots", R.string.Bots));
                CharSequence items[];
                SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                final int tabVal = 5;
                final int sort = plusPreferences.getInt("sortBots", 0);
                final int def = plusPreferences.getInt("defTab", -1);
                CharSequence cs = def == tabVal ? LocaleController.getString("ResetDefaultTab", R.string.ResetDefaultTab) : LocaleController.getString("SetAsDefaultTab", R.string.SetAsDefaultTab);
                items = new CharSequence[]{ cs, LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead)};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = plusPreferences.edit();
                        if (which == 0) {
                            editor.putInt("defTab", def == tabVal ? -1 : tabVal).apply();
                        } else if (which == 1) {
                            markAsReadDialog(true);
                        }
                    }
                });
                showDialog(builder.create());
                return true;
            }
        });
        channelsTab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("Channels", R.string.Channels));
                CharSequence items[];
                SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                final int tabVal = 3;
                final int sort = plusPreferences.getInt("sortChannels", 0);
                final int def = plusPreferences.getInt("defTab", -1);
                CharSequence cs = def == tabVal ? LocaleController.getString("ResetDefaultTab", R.string.ResetDefaultTab) : LocaleController.getString("SetAsDefaultTab", R.string.SetAsDefaultTab);
                items = new CharSequence[]{ cs, LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead)};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = plusPreferences.edit();

                        if (which == 0) {
                            editor.putInt("defTab", def == tabVal ? -1 : tabVal).apply();
                        } else if (which == 1) {
                            markAsReadDialog(true);
                        }

                    }
                });
                showDialog(builder.create());
                return true;
            }
        });
        favsTab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("Favorites", R.string.Favorites));
                CharSequence items[];
                SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                final int tabVal = 8;
                final int sort = plusPreferences.getInt("sortFavs", 0);
                final int def = plusPreferences.getInt("defTab", -1);
                CharSequence cs = def == tabVal ? LocaleController.getString("ResetDefaultTab", R.string.ResetDefaultTab) : LocaleController.getString("SetAsDefaultTab", R.string.SetAsDefaultTab);
                items = new CharSequence[]{ cs, LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead)};
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = plusPreferences.edit();
                        if (which == 0) {
                            editor.putInt("defTab", def == tabVal ? -1 : tabVal).apply();
                        } else if (which == 1) {
                            markAsReadDialog(true);
                        }

                    }
                });
                showDialog(builder.create());
                return true;
            }
        });
        ViewGroup.LayoutParams params = tabsView.getLayoutParams();
        if (params != null) {
            params.height = h;
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            tabsView.setLayoutParams(params);
        }

    }
    private android.app.AlertDialog.Builder createTabsDialog(final Context context, android.app.AlertDialog.Builder builder) {
        builder.setTitle(LocaleController.getString("HideShowTabs", R.string.HideShowTabs));

        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        boolean hideUsers = plusPreferences.getBoolean("hideUsers", false);
        boolean hideGroups = plusPreferences.getBoolean("hideGroups", false);
        boolean hideSGroups = plusPreferences.getBoolean("hideSGroups", false);
        boolean hideChannels = plusPreferences.getBoolean("hideChannels", false);
        boolean hideBots = plusPreferences.getBoolean("hideBots", false);
        boolean hideFavs = plusPreferences.getBoolean("hideFavs", false);

        builder.setMultiChoiceItems(
                new CharSequence[]{LocaleController.getString("Users", R.string.Users), LocaleController.getString("Groups", R.string.Groups), LocaleController.getString("SuperGroups", R.string.SuperGroups), LocaleController.getString("Channels", R.string.Channels), LocaleController.getString("Bots", R.string.Bots), LocaleController.getString("Favorites", R.string.Favorites)},
                new boolean[]{!hideUsers, !hideGroups, !hideSGroups, !hideChannels, !hideBots, !hideFavs},
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = plusPreferences.edit();

                        boolean hide = plusPreferences.getBoolean("hideTabs", false);

                        boolean hideUsers = plusPreferences.getBoolean("hideUsers", false);
                        boolean hideGroups = plusPreferences.getBoolean("hideGroups", false);
                        boolean hideSGroups = plusPreferences.getBoolean("hideSGroups", false);
                        boolean hideChannels = plusPreferences.getBoolean("hideChannels", false);
                        boolean hideBots = plusPreferences.getBoolean("hideBots", false);
                        boolean hideFavs = plusPreferences.getBoolean("hideFavs", false);

                        if (which == 0) {
                            RelativeLayout rl = (RelativeLayout) usersTab.getParent();
                            editor.putBoolean("hideUsers", !hideUsers).apply();
                            if (!hideUsers) {
                                tabsLayout.removeView(rl);
                                if (dialogsType == 3) {
                                    dialogsType = 0;
                                    refreshAdapter(context);
                                }
                                hideUsers = true;
                            } else {
                                tabsLayout.addView(rl, 1, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
                            }
                        } else if (which == 1) {
                            RelativeLayout rl = (RelativeLayout) groupsTab.getParent();
                            editor.putBoolean("hideGroups", !hideGroups).apply();
                            if (!hideGroups) {
                                tabsLayout.removeView(rl);
                                if (dialogsType == 4) {
                                    dialogsType = 0;
                                    refreshAdapter(context);
                                }
                                hideGroups = true;
                            } else {
                                tabsLayout.addView(rl, hideUsers ? 1 : 2, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
                            }
                        } else if (which == 2) {
                            RelativeLayout rl = (RelativeLayout) superGroupsTab.getParent();
                            editor.putBoolean("hideSGroups", !hideSGroups).apply();
                            if (!hideSGroups) {
                                tabsLayout.removeView(rl);
                                if (dialogsType == 7) {
                                    dialogsType = 4;
                                    refreshAdapter(context);
                                }
                                hideSGroups = true;
                            } else {
                                int pos = 3;
                                if (hideUsers) pos = pos - 1;
                                if (hideGroups) pos = pos - 1;
                                tabsLayout.addView(rl, pos, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
                            }
                        } else if (which == 3) {
                            RelativeLayout rl = (RelativeLayout) channelsTab.getParent();
                            editor.putBoolean("hideChannels", !hideChannels).apply();
                            if (!hideChannels) {
                                tabsLayout.removeView(rl);
                                if (dialogsType == 5) {
                                    dialogsType = 0;
                                    refreshAdapter(context);
                                }
                                hideChannels = true;
                            } else {
                                int place = tabsLayout.getChildCount();
                                if (!hideFavs) --place;
                                if (!hideBots) --place;
                                tabsLayout.addView(rl, place, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
                            }
                        } else if (which == 4) {
                            RelativeLayout rl = (RelativeLayout) botsTab.getParent();
                            editor.putBoolean("hideBots", !hideBots).apply();
                            if (!hideBots) {
                                tabsLayout.removeView(rl);
                                if (dialogsType == 6) {
                                    dialogsType = 0;
                                    refreshAdapter(context);
                                }
                                hideBots = true;
                            } else {
                                int place = tabsLayout.getChildCount();
                                if (!hideFavs) --place;
                                tabsLayout.addView(rl, place, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, Gravity.TOP, 0, 0, 0, 0));
                            }
                        } else if (which == 5) {
                            RelativeLayout rl = (RelativeLayout) favsTab.getParent();
                            editor.putBoolean("hideFavs", !hideFavs).apply();
                            if (!hideFavs) {
                                tabsLayout.removeView(rl);
                                if (dialogsType == 8) {
                                    dialogsType = 0;
                                    refreshAdapter(context);
                                }
                                hideFavs = true;
                            } else {
                                tabsLayout.addView(rl, tabsLayout.getChildCount(), LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
                            }
                        }
                        if (hideUsers && hideGroups && hideSGroups && hideChannels && hideBots && hideFavs) {
                            hideTabs = true;
                            editor.putBoolean("hideTabs", true).apply();
                            refreshTabAndListViews(true);
                        }
                        if (isChecked && hide) {
                            hideTabs = false;
                            editor.putBoolean("hideTabs", false).apply();
                            refreshTabAndListViews(false);
                        }
                    }
                });
        return builder;
    }

    private void markAsReadDialog(final boolean all) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setMessage(LocaleController.getString("AreYouSure", R.string.AreYouSure));
        builder.setTitle(all ? LocaleController.getString("MarkAllAsRead", R.string.MarkAllAsRead) : LocaleController.getString("MarkAsRead", R.string.MarkAsRead));
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TLRPC.TL_dialog dialg;
                if (all) {
                    ArrayList<TLRPC.TL_dialog> dialogs = getDialogsArray();
                    if (dialogs != null && !dialogs.isEmpty()) {
                        for (int a = 0; a < dialogs.size(); a++) {
                            dialg = getDialogsArray().get(a);
                            if (dialg.unread_count > 0) {
                                MessagesController.getInstance().markDialogAsRead(dialg.id, dialg.last_read, Math.max(0, dialg.top_message), dialg.last_message_date, true, false);
                            }
                        }
                        return;
                    }
                    return;
                }
                dialg = MessagesController.getInstance().dialogs_dict.get(selectedDialog);
                if (dialg.unread_count > 0) {
                    MessagesController.getInstance().markDialogAsRead(dialg.id, dialg.last_read, Math.max(0, dialg.top_message), dialg.last_message_date, true, false);
                }
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void updateTabs() {
        hideTabs = preferences.getBoolean("hideTabs", false);
        disableAnimation = preferences.getBoolean("disableTabsAnimation", false);
        tabsHeight = preferences.getInt("tabsHeight", 40);
        refreshTabAndListViews(false);
        if (hideTabs && dialogsType > 2) {
            dialogsType = 0;
            refreshAdapterAndTabs(dialogsBackupAdapter);
        }

    }
    private void refreshTabAndListViews(boolean forceHide) {
        if (hideTabs || forceHide) {
            tabsView.setVisibility(View.GONE);
            listView.setPadding(0, 0, 0, 0);
        } else {
            tabsView.setVisibility(View.VISIBLE);
            int h = AndroidUtilities.dp((float) 40);
            ViewGroup.LayoutParams params = tabsView.getLayoutParams();
            if (params != null) {
                params.height = h;
                tabsView.setLayoutParams(params);
            }
            listView.setPadding(0, preferences.getBoolean("tabsToBottom", false) ? 10 : h, 0, preferences.getBoolean("tabsToBottom", false) ? h : 0);
            hideTabsAnimated(false);
        }
        listView.scrollToPosition(0);
    }
    private void refreshAdapter(Context context) {
        refreshAdapterAndTabs(new DialogsAdapter(context, dialogsType));
    }
    private void refreshAdapterAndTabs(DialogsAdapter adapter) {
        dialogsAdapter = adapter;
        listView.setAdapter(dialogsAdapter);
        dialogsAdapter.notifyDataSetChanged();
        if (!onlySelect) {
            preferences.edit().putInt("selTab", dialogsType == 9 ? 3 : dialogsType).apply();
        }
        refreshTabs();
    }
    private void refreshTabs() {
        int i;
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        int defColor = themePrefs.getInt("chatsHeaderIconsColor", 0xffffffff);
        int iconColor = themePrefs.getInt("chatsHeaderTabIconColor", defColor);
        int iColor = themePrefs.getInt("chatsHeaderTabUnselectedIconColor", AndroidUtilities.getIntAlphaColor("chatsHeaderTabIconColor", defColor, 0.3f));

        allTab.setBackgroundResource(0);
        usersTab.setBackgroundResource(0);
        groupsTab.setBackgroundResource(0);
        superGroupsTab.setBackgroundResource(0);
        channelsTab.setBackgroundResource(0);
        botsTab.setBackgroundResource(0);
        favsTab.setBackgroundResource(0);

        allTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        usersTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        groupsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        superGroupsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        channelsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        botsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        favsTab.setColorFilter(iColor, PorterDuff.Mode.SRC_IN);
        Drawable selected = getParentActivity().getResources().getDrawable(R.drawable.tab_selected);
        selected.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
        if (dialogsType == 9) {
            i = 0;
        } else {
            i = dialogsType;
        }
        switch (i) {
            case 6:
                usersTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                usersTab.setBackgroundDrawable(selected);
                break;
            case 4:
                groupsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                groupsTab.setBackgroundDrawable(selected);
                break;
            case 3:
                channelsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                channelsTab.setBackgroundDrawable(selected);
                break;
            case 5:
                botsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                botsTab.setBackgroundDrawable(selected);
                break;
            case 7:
                superGroupsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                superGroupsTab.setBackgroundDrawable(selected);
                break;
            case 8:
                favsTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                favsTab.setBackgroundDrawable(selected);
                break;
            default:
                allTab.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
                allTab.setBackgroundDrawable(selected);
                break;
        }
        String t = getHeaderAllTitles();
        actionBar.setTitle(t);
        paintHeader(true);
        if (getDialogsArray() != null && getDialogsArray().isEmpty()) {
            searchEmptyView.setVisibility(View.GONE);
            progressView.setVisibility(View.GONE);
            if (emptyView.getChildCount() > 0) {
                TextView tv = (TextView) emptyView.getChildAt(0);
                tv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
                if (tv != null) {
                    tv.setText(dialogsType < 3 ? LocaleController.getString("NoChats", R.string.NoChats) : dialogsType == 8 ? LocaleController.getString("NoFavoritesHelp", R.string.NoFavoritesHelp) : t);
                    tv.setTextColor(themePrefs.getInt("chatsNameColor", 0xff212121));
                }
                if (emptyView.getChildAt(1) != null)
                    emptyView.getChildAt(1).setVisibility(View.GONE);
            }
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setBackgroundColor(themePrefs.getInt("chatsRowColor", 0xffffffff));
            listView.setEmptyView(emptyView);
        }
    }

    private String getHeaderAllTitles() {
        switch (dialogsType) {
            case 6:
                return LocaleController.getString("Users", R.string.Users);
            case 4:
                return LocaleController.getString("Groups", R.string.Groups);
            case 3:
                return LocaleController.getString("Channels", R.string.Channels);
            case 5:
                return LocaleController.getString("Bots", R.string.Bots);
            case 7:
                return LocaleController.getString("SuperGroups", R.string.SuperGroups);
            case 8:
                return LocaleController.getString("Favorites", R.string.Favorites);
            default:
                return getHeaderTitle();
        }
    }
    private String getHeaderTitle() {
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        int value = themePrefs.getInt("chatsHeaderTitle", 0);
        String title = LocaleController.getString("AppName", R.string.AppName);
        TLRPC.User user = UserConfig.getCurrentUser();
        if (value == 1) {
            title = LocaleController.getString("ShortAppName", R.string.ShortAppName);
        } else if (value == 2) {
            if (user != null && (user.first_name != null || user.last_name != null)) {
                title = ContactsController.formatName(user.first_name, user.last_name);
            }
        } else if (value == 3) {
            if (user != null && user.username != null && user.username.length() != 0) {
                title = "@" + user.username;
            }
        } else if (value == 4) {
            title = "";
        }
        return title;
    }

    private void paintHeader(boolean tabs) {
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(AndroidUtilities.THEME_PREFS, AndroidUtilities.THEME_PREFS_MODE);
        actionBar.setTitleColor(themePrefs.getInt("chatsHeaderTitleColor", 0xffffffff));
        int def = themePrefs.getInt("themeColor", AndroidUtilities.defColor);
        int hColor = themePrefs.getInt("chatsHeaderColor", def);
        /*if(!tabs){
            actionBar.setBackgroundColor(hColor);
        }else{
            tabsView.setBackgroundColor(hColor);
        }*/
        if (!tabs) actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
        if (tabs) {
            tabsView.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
        }
        int val = themePrefs.getInt("chatsHeaderGradient", 0);
        if (val > 0) {
            GradientDrawable.Orientation go;
            switch (val) {
                case 2:
                    go = GradientDrawable.Orientation.LEFT_RIGHT;
                    break;
                case 3:
                    go = GradientDrawable.Orientation.TL_BR;
                    break;
                case 4:
                    go = GradientDrawable.Orientation.BL_TR;
                    break;
                default:
                    go = GradientDrawable.Orientation.TOP_BOTTOM;
            }
            int gradColor = themePrefs.getInt("chatsHeaderGradientColor", def);
            int[] colors = new int[]{hColor, gradColor};
            GradientDrawable gd = new GradientDrawable(go, colors);
            if (!tabs) actionBar.setBackgroundDrawable(gd);
            if (tabs) {
                tabsView.setBackgroundDrawable(gd);
            }
            /*if(!tabs){
                actionBar.setBackgroundDrawable(gd);
            }else{
                tabsView.setBackgroundDrawable(gd);
            }*/
        }
    }

    private void hideTabsAnimated(boolean hide) {
        int i = 1;
        if (tabsHidden != hide) {
            float f;
            tabsHidden = hide;
            if (hide) {
                listView.setPadding(0, 0, 0, 0);
            }
            FrameLayout frameLayout = tabsView;
            String str = "translationY";
            float[] fArr = new float[1];
            if (hide) {
                int i2 = -AndroidUtilities.dp((float) tabsHeight);
                if (preferences.getBoolean("tabsToBottom", false)) {
                    i = -1;
                }
                f = (float) (i * i2);
            } else {
                f = 0.0f;
            }
            fArr[0] = f;
            /*ObjectAnimator animator = ObjectAnimator.ofFloat(frameLayout, str, fArr).setDuration(300);
            animator.addListener(new AnimatorListenerAdapterProxy() {
                public void onAnimationEnd(Animator animation) {
                    if (!tabsHidden) {
                        listView.setPadding(0, preferences.getBoolean("tabsToBottom", false) ? 0 : AndroidUtilities.dp((float) tabsHeight), 0, preferences.getBoolean("tabsToBottom", false) ? AndroidUtilities.dp((float) tabsHeight) : 0);
                    }
                }
            });
            animator.start();*/
        }
    }
    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);

        if (getArguments() != null) {
            onlySelect = arguments.getBoolean("onlySelect", false);
            cantSendToChannels = arguments.getBoolean("cantSendToChannels", false);
            dialogsType = arguments.getInt("dialogsType", 0);
            selectAlertString = arguments.getString("selectAlertString");
            selectAlertStringGroup = arguments.getString("selectAlertStringGroup");
            addToGroupAlertString = arguments.getString("addToGroupAlertString");
        }

        if (searchString == null) {
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.dialogsNeedReload);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.emojiDidLoaded);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.updateInterfaces);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.encryptedChatUpdated);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.contactsDidLoaded);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.appDidLogout);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.openedChatChanged);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.notificationsSettingsUpdated);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageReceivedByAck);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageReceivedByServer);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.messageSendError);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.didSetPasscode);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.needReloadRecentDialogsSearch);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.didLoadedReplyMessages);
            NotificationCenter.getInstance().addObserver(this, NotificationCenter.reloadHints);
        }


        if (!dialogsLoaded) {
            MessagesController.getInstance().loadDialogs(0, 100, true);
            ContactsController.getInstance().checkInviteText();
            MessagesController.getInstance().loadPinnedDialogs(0, null);
            StickersQuery.checkFeaturedStickers();
            dialogsLoaded = true;
        }
        return true;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (searchString == null) {
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.dialogsNeedReload);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.emojiDidLoaded);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.updateInterfaces);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.encryptedChatUpdated);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.contactsDidLoaded);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.appDidLogout);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.openedChatChanged);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.notificationsSettingsUpdated);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageReceivedByAck);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageReceivedByServer);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.messageSendError);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didSetPasscode);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.needReloadRecentDialogsSearch);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.didLoadedReplyMessages);
            NotificationCenter.getInstance().removeObserver(this, NotificationCenter.reloadHints);
        }
        delegate = null;
    }

    @Override
    public View createView(final Context context) {
        searching = false;
        searchWas = false;
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);

        chatUnlocked = preferences.getBoolean("chat_unlocked", false);
        chatPassword = preferences.getString("chat_password", "");

        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                Theme.createChatResources(context, false);
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        if (!onlySelect && searchString == null) {
            passcodeItem = menu.addItem(1, R.drawable.lock_close);
            updatePasscodeButton();
        }
        final ActionBarMenuItem item = menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                searching = true;
                if (listView != null) {
                    if (searchString != null) {
                        listView.setEmptyView(searchEmptyView);
                        progressView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.GONE);
                    }
                    if (!onlySelect) {
                        floatingButton.setVisibility(View.GONE);
                    }
                }
                updatePasscodeButton();
            }

            @Override
            public boolean canCollapseSearch() {
                if (searchString != null) {
                    finishFragment();
                    return false;
                }
                return true;
            }

            @Override
            public void onSearchCollapse() {
                searching = false;
                searchWas = false;
                if (listView != null) {
                    searchEmptyView.setVisibility(View.GONE);
                    if (MessagesController.getInstance().loadingDialogs && MessagesController.getInstance().dialogs.isEmpty()) {
                        emptyView.setVisibility(View.GONE);
                        listView.setEmptyView(progressView);
                    } else {
                        progressView.setVisibility(View.GONE);
                        listView.setEmptyView(emptyView);
                    }
                    if (!onlySelect) {
                        floatingButton.setVisibility(View.VISIBLE);
                        floatingHidden = true;
                        floatingButton.setTranslationY(AndroidUtilities.dp(100));
                        hideFloatingButton(false);
                    }
                    if (listView.getAdapter() != dialogsAdapter) {
                        listView.setAdapter(dialogsAdapter);
                        dialogsAdapter.notifyDataSetChanged();
                    }
                }
                if (dialogsSearchAdapter != null) {
                    dialogsSearchAdapter.searchDialogs(null);
                }
                updatePasscodeButton();
            }

            @Override
            public void onTextChanged(EditText editText) {
                String text = editText.getText().toString();
                if (text.length() != 0 || dialogsSearchAdapter != null && dialogsSearchAdapter.hasRecentRearch()) {
                    searchWas = true;
                    if (dialogsSearchAdapter != null && listView.getAdapter() != dialogsSearchAdapter) {
                        listView.setAdapter(dialogsSearchAdapter);
                        dialogsSearchAdapter.notifyDataSetChanged();
                    }
                    if (searchEmptyView != null && listView.getEmptyView() != searchEmptyView) {
                        emptyView.setVisibility(View.GONE);
                        progressView.setVisibility(View.GONE);
                        searchEmptyView.showTextView();
                        listView.setEmptyView(searchEmptyView);
                    }
                }
                if (dialogsSearchAdapter != null) {
                    dialogsSearchAdapter.searchDialogs(text);
                }
            }
        });

        item.getSearchField().setHint(LocaleController.getString("Search", R.string.Search));
        if (onlySelect) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            actionBar.setTitle(LocaleController.getString("SelectChat", R.string.SelectChat));
        } else {
            if (searchString != null) {
                actionBar.setBackButtonImage(R.drawable.ic_ab_back);
            } else {
                actionBar.setBackButtonDrawable(new MenuDrawable());
            }
            if (BuildVars.DEBUG_VERSION) {
                actionBar.setTitle(LocaleController.getString("AppNameBeta", R.string.AppNameBeta));
            } else {
                actionBar.setTitle(LocaleController.getString("AppName", R.string.AppName));
            }
            if (preferences.getBoolean("show_ghost_state_icon", true)) {
                if (preferences.getBoolean("ghost_mode", false)) {
                    Drawable ic_ghost = getParentActivity().getResources().getDrawable(R.drawable.ic_ghost);
                    ghostItem = menu.addItem(3, ic_ghost);

                } else {
                    Drawable ic_ghost_disable = getParentActivity().getResources().getDrawable(R.drawable.ic_ghost_disable);
                    if (ic_ghost_disable != null)
                        ghostItem = menu.addItem(3, ic_ghost_disable);

                }
            } else {
                ghostItem = menu.addItem(3, R.color.transparent);
            }

            if (preferences.getBoolean("show_last_seen_icon", false)) {
                Drawable ic_teleh_seen = getParentActivity().getResources().getDrawable(R.drawable.ic_teleh_seen);
                if (ic_teleh_seen != null)
                    menu.addItem(2, ic_teleh_seen);
            }
        }
        actionBar.setAllowOverlayTitle(true);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (onlySelect) {
                        finishFragment();
                    } else if (parentLayout != null) {
                        parentLayout.getDrawerLayoutContainer().openDrawer(false);
                    }
                } else if (id == 1) {
                    UserConfig.appLocked = !UserConfig.appLocked;
                    UserConfig.saveConfig(false);
                    updatePasscodeButton();
                }
                else if (id == 3) {
                    changeGhostModeState();
                }
            }
        });

        if (sideMenu != null) {
            sideMenu.setBackgroundColor(Theme.getColor(Theme.key_chats_menuBackground));
            sideMenu.setGlowColor(Theme.getColor(Theme.key_chats_menuBackground));
            sideMenu.getAdapter().notifyDataSetChanged();
        }

        FrameLayout frameLayout = new FrameLayout(context);
        fragmentView = frameLayout;

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(true);
        listView.setItemAnimator(null);
        listView.setInstantClick(true);
        listView.setLayoutAnimation(null);
        listView.setPadding(0, preferences.getBoolean("tabsToBottom", false) ? 10 : h, 0, preferences.getBoolean("tabsToBottom", false) ? h : 0);
        listView.setTag(4);
        layoutManager = new LinearLayoutManager(context) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        listView.setLayoutManager(layoutManager);
        listView.setVerticalScrollbarPosition(LocaleController.isRTL ? RecyclerListView.SCROLLBAR_POSITION_LEFT : RecyclerListView.SCROLLBAR_POSITION_RIGHT);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setOnItemClickListener(new RecyclerListView.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (listView == null || listView.getAdapter() == null) {
                    return;
                }
                long dialog_id = 0;
                int message_id = 0;
                RecyclerView.Adapter adapter = listView.getAdapter();
                if (adapter == dialogsAdapter) {
                    TLRPC.TL_dialog dialog = dialogsAdapter.getItem(position);
                    if (dialog == null) {
                        return;
                    }
                    dialog_id = dialog.id;
                } else if (adapter == dialogsSearchAdapter) {
                    Object obj = dialogsSearchAdapter.getItem(position);
                    if (obj instanceof TLRPC.User) {
                        dialog_id = ((TLRPC.User) obj).id;
                        if (dialogsSearchAdapter.isGlobalSearch(position)) {
                            ArrayList<TLRPC.User> users = new ArrayList<>();
                            users.add((TLRPC.User) obj);
                            MessagesController.getInstance().putUsers(users, false);
                            MessagesStorage.getInstance().putUsersAndChats(users, null, false, true);
                        }
                        if (!onlySelect) {
                            dialogsSearchAdapter.putRecentSearch(dialog_id, (TLRPC.User) obj);
                        }
                    } else if (obj instanceof TLRPC.Chat) {
                        if (dialogsSearchAdapter.isGlobalSearch(position)) {
                            ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                            chats.add((TLRPC.Chat) obj);
                            MessagesController.getInstance().putChats(chats, false);
                            MessagesStorage.getInstance().putUsersAndChats(null, chats, false, true);
                        }
                        if (((TLRPC.Chat) obj).id > 0) {
                            dialog_id = -((TLRPC.Chat) obj).id;
                        } else {
                            dialog_id = AndroidUtilities.makeBroadcastId(((TLRPC.Chat) obj).id);
                        }
                        if (!onlySelect) {
                            dialogsSearchAdapter.putRecentSearch(dialog_id, (TLRPC.Chat) obj);
                        }
                    } else if (obj instanceof TLRPC.EncryptedChat) {
                        dialog_id = ((long) ((TLRPC.EncryptedChat) obj).id) << 32;
                        if (!onlySelect) {
                            dialogsSearchAdapter.putRecentSearch(dialog_id, (TLRPC.EncryptedChat) obj);
                        }
                    } else if (obj instanceof MessageObject) {
                        MessageObject messageObject = (MessageObject) obj;
                        dialog_id = messageObject.getDialogId();
                        message_id = messageObject.getId();
                        dialogsSearchAdapter.addHashtagsFromMessage(dialogsSearchAdapter.getLastSearchString());
                    } else if (obj instanceof String) {
                        actionBar.openSearchField((String) obj);
                    }
                }

                if (dialog_id == 0) {
                    return;
                }

                if (onlySelect) {
                    didSelectResult(dialog_id, true, false);
                } else {
                    Bundle args = new Bundle();
                    int lower_part = (int) dialog_id;
                    int high_id = (int) (dialog_id >> 32);
                    if (lower_part != 0) {
                        if (high_id == 1) {
                            args.putInt("chat_id", lower_part);
                        } else {
                            if (lower_part > 0) {
                                args.putInt("user_id", lower_part);
                            } else if (lower_part < 0) {
                                if (message_id != 0) {
                                    TLRPC.Chat chat = MessagesController.getInstance().getChat(-lower_part);
                                    if (chat != null && chat.migrated_to != null) {
                                        args.putInt("migrated_to", lower_part);
                                        lower_part = -chat.migrated_to.channel_id;
                                    }
                                }
                                args.putInt("chat_id", -lower_part);
                            }
                        }
                    } else {
                        args.putInt("enc_id", high_id);
                    }
                    if (message_id != 0) {
                        args.putInt("message_id", message_id);
                    } else {
                        if (actionBar != null) {
                            actionBar.closeSearchField();
                        }
                    }
                    if (AndroidUtilities.isTablet()) {
                        if (openedDialogId == dialog_id && adapter != dialogsSearchAdapter) {
                            return;
                        }
                        if (dialogsAdapter != null) {
                            dialogsAdapter.setOpenedDialogId(openedDialogId = dialog_id);
                            updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
                        }
                    }
                    if (searchString != null) {
                        if (MessagesController.checkCanOpenChat(args, DialogsActivity.this)) {
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                            presentFragment(new ChatActivity(args));
                        }
                    } else {
                        if (MessagesController.checkCanOpenChat(args, DialogsActivity.this)) {
                            presentFragment(new ChatActivity(args));
                        }
                    }
                }
            }
        });
        listView.setOnItemLongClickListener(new RecyclerListView.OnItemLongClickListener() {
            @Override
            public boolean onItemClick(View view, int position) {
                if (onlySelect || searching && searchWas || getParentActivity() == null) {
                    if (searchWas && searching || dialogsSearchAdapter.isRecentSearchDisplayed()) {
                        RecyclerView.Adapter adapter = listView.getAdapter();
                        if (adapter == dialogsSearchAdapter) {
                            Object item = dialogsSearchAdapter.getItem(position);
                            if (item instanceof String || dialogsSearchAdapter.isRecentSearchDisplayed()) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                                builder.setMessage(LocaleController.getString("ClearSearch", R.string.ClearSearch));
                                builder.setPositiveButton(LocaleController.getString("ClearButton", R.string.ClearButton).toUpperCase(), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        if (dialogsSearchAdapter.isRecentSearchDisplayed()) {
                                            dialogsSearchAdapter.clearRecentSearch();
                                        } else {
                                            dialogsSearchAdapter.clearRecentHashtags();
                                        }
                                    }
                                });
                                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                                showDialog(builder.create());
                                return true;
                            }
                        }
                    }
                    return false;
                }
                TLRPC.TL_dialog dialog;
                ArrayList<TLRPC.TL_dialog> dialogs = getDialogsArray();
                if (position < 0 || position >= dialogs.size()) {
                    return false;
                }
                dialog = dialogs.get(position);
                selectedDialog = dialog.id;
                final boolean pinned = dialog.pinned;

                BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());
                int lower_id = (int) selectedDialog;
                int high_id = (int) (selectedDialog >> 32);

                if (DialogObject.isChannel(dialog)) {
                    final TLRPC.Chat chat = MessagesController.getInstance().getChat(-lower_id);
                    CharSequence items[];
                    int icons[] = new int[]{
                            dialog.pinned ? R.drawable.chats_unpin : R.drawable.chats_pin,
                            R.drawable.chats_clear,
                            R.drawable.chats_delete,
                            R.drawable.tab_favs
                    };
                    if (chat != null && chat.megagroup) {
                        items = new CharSequence[]{
                                dialog.pinned || MessagesController.getInstance().canPinDialog(false) ? (dialog.pinned ? LocaleController.getString("UnpinFromTop", R.string.UnpinFromTop) : LocaleController.getString("PinToTop", R.string.PinToTop)) : null,
                                LocaleController.getString("ClearHistoryCache", R.string.ClearHistoryCache),
                                chat == null || !chat.creator ? LocaleController.getString("LeaveMegaMenu", R.string.LeaveMegaMenu) : LocaleController.getString("DeleteMegaMenu", R.string.DeleteMegaMenu),
                                Favourite.isFavourite(selectedDialog) ? LocaleController.getString("DeleteFromFavorites", R.string.DeleteFromFavorites) : LocaleController.getString("AddToFavorites", R.string.AddToFavorites)
                                };
                    } else {
                        items = new CharSequence[]{
                                dialog.pinned || MessagesController.getInstance().canPinDialog(false) ? (dialog.pinned ? LocaleController.getString("UnpinFromTop", R.string.UnpinFromTop) : LocaleController.getString("PinToTop", R.string.PinToTop)) : null,
                                LocaleController.getString("ClearHistoryCache", R.string.ClearHistoryCache),
                                chat == null || !chat.creator ? LocaleController.getString("LeaveChannelMenu", R.string.LeaveChannelMenu) : LocaleController.getString("ChannelDeleteMenu", R.string.ChannelDeleteMenu)
                                , Favourite.isFavourite(selectedDialog) ? LocaleController.getString("DeleteFromFavorites", R.string.DeleteFromFavorites) : LocaleController.getString("AddToFavorites", R.string.AddToFavorites)};
                    }
                    builder.setItems(items, icons, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, final int which) {
                            final boolean isHide = preferences.contains("hide_" + String.valueOf(selectedDialog));
                            final boolean chatUnlocked = preferences.getBoolean("chat_unlocked", false);

                            if (which == 3) {
                                TLRPC.TL_dialog localDialog2 = MessagesController.getInstance().dialogs_dict.get(selectedDialog);

                                if (Favourite.isFavourite(selectedDialog)) {
                                    Favourite.deleteFavourite(selectedDialog);
                                    MessagesController.getInstance().dialogsFavs.remove(localDialog2);
                                    dialogsAdapter.notifyDataSetChanged();
                                } else if (!Favourite.isFavourite(selectedDialog)) {
                                    Favourite.addFavourite(selectedDialog);
                                    MessagesController.getInstance().dialogsFavs.add(localDialog2);
                                }
                            }
                            else if (which == 0) {
                                if (MessagesController.getInstance().pinDialog(selectedDialog, !pinned, null, 0) && !pinned) {
                                    listView.smoothScrollToPosition(0);
                                }
                            } else if(which !=0 && which!=3 && which!=4)  {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                                if (which == 1) {
                                    if (chat != null && chat.megagroup) {
                                        builder.setMessage(LocaleController.getString("AreYouSureClearHistorySuper", R.string.AreYouSureClearHistorySuper));
                                    } else {
                                        builder.setMessage(LocaleController.getString("AreYouSureClearHistoryChannel", R.string.AreYouSureClearHistoryChannel));
                                    }
                                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            MessagesController.getInstance().deleteDialog(selectedDialog, 2);
                                        }
                                    });
                                } else if (which == 2){
                                    if (chat != null && chat.megagroup) {
                                        if (!chat.creator) {
                                            builder.setMessage(LocaleController.getString("MegaLeaveAlert", R.string.MegaLeaveAlert));
                                        } else {
                                            builder.setMessage(LocaleController.getString("MegaDeleteAlert", R.string.MegaDeleteAlert));
                                        }
                                    } else {
                                        if (chat == null || !chat.creator) {
                                            builder.setMessage(LocaleController.getString("ChannelLeaveAlert", R.string.ChannelLeaveAlert));
                                        } else {
                                            builder.setMessage(LocaleController.getString("ChannelDeleteAlert", R.string.ChannelDeleteAlert));
                                        }
                                    }
                                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            MessagesController.getInstance().deleteUserFromChat((int) -selectedDialog, UserConfig.getCurrentUser(), null);
                                            if (AndroidUtilities.isTablet()) {
                                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats, selectedDialog);
                                            }
                                        }
                                    });
                                }
                                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                                showDialog(builder.create());
                            }
                        }
                    });
                    showDialog(builder.create());
                } else {
                    final boolean isChat = lower_id < 0 && high_id != 1;
                    TLRPC.User user = null;
                    if (!isChat && lower_id > 0 && high_id != 1) {
                        user = MessagesController.getInstance().getUser(lower_id);
                    }
                    final boolean isBot = user != null && user.bot;

                    builder.setItems(new CharSequence[]{
                            dialog.pinned || MessagesController.getInstance().canPinDialog(lower_id == 0) ? (dialog.pinned ? LocaleController.getString("UnpinFromTop", R.string.UnpinFromTop) : LocaleController.getString("PinToTop", R.string.PinToTop)) : null,
                            LocaleController.getString("ClearHistory", R.string.ClearHistory),
                            isChat ? LocaleController.getString("DeleteChat", R.string.DeleteChat) : isBot ? LocaleController.getString("DeleteAndStop", R.string.DeleteAndStop) : LocaleController.getString("Delete", R.string.Delete)
                            , Favourite.isFavourite(selectedDialog) ? LocaleController.getString("DeleteFromFavorites", R.string.DeleteFromFavorites) : LocaleController.getString("AddToFavorites", R.string.AddToFavorites)

                    }, new int[]{
                            dialog.pinned ? R.drawable.chats_unpin : R.drawable.chats_pin,
                            R.drawable.chats_clear,
                            isChat ? R.drawable.chats_leave : R.drawable.chats_delete, R.drawable.tab_favs, R.drawable.lock_open
                    }, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, final int which) {
                            if (which == 0) {
                                if (MessagesController.getInstance().pinDialog(selectedDialog, !pinned, null, 0) && !pinned) {
                                    listView.smoothScrollToPosition(0);
                                }
                            }
                            else if (which == 3) {
                                TLRPC.TL_dialog localDialog2 = MessagesController.getInstance().dialogs_dict.get(selectedDialog);

                                if (Favourite.isFavourite(Long.valueOf(selectedDialog))) {
                                    Favourite.deleteFavourite(Long.valueOf(selectedDialog));
                                    MessagesController.getInstance().dialogsFavs.remove(localDialog2);
                                    dialogsAdapter.notifyDataSetChanged();
                                } else if (!Favourite.isFavourite(Long.valueOf(selectedDialog))) {
                                    Favourite.addFavourite(Long.valueOf(selectedDialog));
                                    MessagesController.getInstance().dialogsFavs.add(localDialog2);
                                }
                            }
                            else  {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                                if (which == 1) {
                                    builder.setMessage(LocaleController.getString("AreYouSureClearHistory", R.string.AreYouSureClearHistory));
                                } else if (which == 2){
                                    if (isChat) {
                                        builder.setMessage(LocaleController.getString("AreYouSureDeleteAndExit", R.string.AreYouSureDeleteAndExit));
                                    } else {
                                        builder.setMessage(LocaleController.getString("AreYouSureDeleteThisChat", R.string.AreYouSureDeleteThisChat));
                                    }
                                }
                                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        if (which != 1) {
                                            if (isChat) {
                                                TLRPC.Chat currentChat = MessagesController.getInstance().getChat((int) -selectedDialog);
                                                if (currentChat != null && ChatObject.isNotInChat(currentChat)) {
                                                    MessagesController.getInstance().deleteDialog(selectedDialog, 0);
                                                } else {
                                                    MessagesController.getInstance().deleteUserFromChat((int) -selectedDialog, MessagesController.getInstance().getUser(UserConfig.getClientUserId()), null);
                                                }
                                            } else {
                                                MessagesController.getInstance().deleteDialog(selectedDialog, 0);
                                            }
                                            if (isBot) {
                                                MessagesController.getInstance().blockUser((int) selectedDialog);
                                            }
                                            if (AndroidUtilities.isTablet()) {
                                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats, selectedDialog);
                                            }
                                        } else {
                                            MessagesController.getInstance().deleteDialog(selectedDialog, 1);
                                        }
                                    }
                                });
                                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                                showDialog(builder.create());
                            }
                        }
                    });
                    showDialog(builder.create());
                }
                return true;
            }
        });

        searchEmptyView = new EmptyTextProgressView(context);
        searchEmptyView.setVisibility(View.GONE);
        searchEmptyView.setShowAtCenter(true);
        searchEmptyView.setText(LocaleController.getString("NoResult", R.string.NoResult));
        frameLayout.addView(searchEmptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        emptyView = new LinearLayout(context);
        emptyView.setOrientation(LinearLayout.VERTICAL);
        emptyView.setVisibility(View.GONE);
        emptyView.setGravity(Gravity.CENTER);
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        emptyView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        emptyTextView1 = new TextView(context);
        emptyTextView1.setText(LocaleController.getString("NoChats", R.string.NoChats));
        emptyTextView1.setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder));
        emptyTextView1.setGravity(Gravity.CENTER);
        emptyTextView1.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        emptyView.addView(emptyTextView1, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        emptyTextView2 = new TextView(context);
        String help = LocaleController.getString("NoChatsHelp", R.string.NoChatsHelp);
        if (AndroidUtilities.isTablet() && !AndroidUtilities.isSmallTablet()) {
            help = help.replace('\n', ' ');
        }
        emptyTextView2.setText(help);
        emptyTextView2.setTextColor(Theme.getColor(Theme.key_emptyListPlaceholder));
        emptyTextView2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        emptyTextView2.setGravity(Gravity.CENTER);
        emptyTextView2.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), 0);
        emptyTextView2.setLineSpacing(AndroidUtilities.dp(2), 1);
        emptyView.addView(emptyTextView2, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        progressView = new RadialProgressView(context);
        progressView.setVisibility(View.GONE);
        frameLayout.addView(progressView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        floatingButton = new ImageView(context);
        floatingButton.setVisibility(onlySelect ? View.GONE : View.VISIBLE);
        floatingButton.setScaleType(ImageView.ScaleType.CENTER);

        Drawable drawable = Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(56), Theme.getColor(Theme.key_chats_actionBackground), Theme.getColor(Theme.key_chats_actionPressedBackground));
        if (Build.VERSION.SDK_INT < 21) {
            Drawable shadowDrawable = context.getResources().getDrawable(R.drawable.floating_shadow).mutate();
            shadowDrawable.setColorFilter(new PorterDuffColorFilter(0xff000000, PorterDuff.Mode.MULTIPLY));
            CombinedDrawable combinedDrawable = new CombinedDrawable(shadowDrawable, drawable, 0, 0);
            combinedDrawable.setIconSize(AndroidUtilities.dp(56), AndroidUtilities.dp(56));
            drawable = combinedDrawable;
        }
        floatingButton.setBackgroundDrawable(drawable);
        floatingButton.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chats_actionIcon), PorterDuff.Mode.MULTIPLY));
        floatingButton.setImageResource(R.drawable.floating_pencil);
        if (Build.VERSION.SDK_INT >= 21) {
            StateListAnimator animator = new StateListAnimator();
            animator.addState(new int[]{android.R.attr.state_pressed}, ObjectAnimator.ofFloat(floatingButton, "translationZ", AndroidUtilities.dp(2), AndroidUtilities.dp(4)).setDuration(200));
            animator.addState(new int[]{}, ObjectAnimator.ofFloat(floatingButton, "translationZ", AndroidUtilities.dp(4), AndroidUtilities.dp(2)).setDuration(200));
            floatingButton.setStateListAnimator(animator);
            floatingButton.setOutlineProvider(new ViewOutlineProvider() {
                @SuppressLint("NewApi")
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, AndroidUtilities.dp(56), AndroidUtilities.dp(56));
                }
            });
        }
        boolean tabsToBottom = preferences.getBoolean("tabsToBottom", false);

        frameLayout.addView(floatingButton, LayoutHelper.createFrame(Build.VERSION.SDK_INT >= 21 ? 56 : 60, Build.VERSION.SDK_INT >= 21 ? 56 : 60, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.BOTTOM, LocaleController.isRTL ? 14 : 0, 0, LocaleController.isRTL ? 0 : 14, 14));
        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putBoolean("destroyAfterSelect", true);
                presentFragment(new ContactsActivity(args));
            }
        });
        boolean isTabsEnabled = preferences.getBoolean("tabs", true);
        boolean moveTabs = preferences.getBoolean("move_tabs", false);
        boolean swipeTabs = preferences.getBoolean("swipe_tabs", true);
        if (swipeTabs) {
            listView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    displayMetrics = context.getResources().getDisplayMetrics();
                    vDPI = displayMetrics.xdpi / 160.0f;
                    int i = 1;
                    touchPositionDP = (float) Math.round(event.getX() / vDPI);
                    if (hideTabs) {
                        return false;
                    }
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            downX = (float) Math.round(event.getX() / vDPI);
                            downY = (float) Math.round(event.getY() / vDPI);
                            if (downX > 50.0f) {
                                parentLayout.getDrawerLayoutContainer().setAllowOpenDrawer(false, false);
                            }
                            return view instanceof LinearLayout;
                        case MotionEvent.ACTION_UP:
                            upX = (float) Math.round(event.getX() / vDPI);
                            upY = (float) Math.round(event.getY() / vDPI);
                            float deltaX = downX - upX;
                            float deltaY = downY - upY;
                            if (Math.abs(deltaX) > 40.0f && Math.abs(deltaY) < BitmapDescriptorFactory.HUE_YELLOW) {
                                refreshDialogType(deltaX < 0.0f ? 0 : 1);
                                downX = (float) Math.round(event.getX() / vDPI);
                                refreshAdapter(context);
                                refreshTabAndListViews(false);
                            }
                            if (downX <= 50.0f) {
                                return false;
                            }
                            parentLayout.getDrawerLayoutContainer().setAllowOpenDrawer(true, false);
                            return false;
                        default:
                            return false;
                    }
                }
            });
            emptyView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    displayMetrics = context.getResources().getDisplayMetrics();
                    vDPI = displayMetrics.xdpi / 160.0f;
                    int i = 1;
                    touchPositionDP = (float) Math.round(event.getX() / vDPI);
                    if (hideTabs) {
                        return false;
                    }
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN/*0*/:
                            downX = (float) Math.round(event.getX() / vDPI);
                            downY = (float) Math.round(event.getY() / vDPI);
                            if (downX > 50.0f) {
                                parentLayout.getDrawerLayoutContainer().setAllowOpenDrawer(false, false);
                            }
                            return view instanceof LinearLayout;
                        case MotionEvent.ACTION_UP /*1*/:
                            upX = (float) Math.round(event.getX() / vDPI);
                            upY = (float) Math.round(event.getY() / vDPI);
                            float deltaX = downX - upX;
                            float deltaY = downY - upY;
                            if (Math.abs(deltaX) > 40.0f && Math.abs(deltaY) < BitmapDescriptorFactory.HUE_YELLOW) {
                                refreshDialogType(deltaX < 0.0f ? 0 : 1);
                                downX = (float) Math.round(event.getX() / vDPI);
                                refreshAdapter(context);
                                refreshTabAndListViews(false);
                            }
                            if (downX <= 50.0f) {
                                return false;
                            }
                            parentLayout.getDrawerLayoutContainer().setAllowOpenDrawer(true, false);
                            return false;
                        default:
                            return false;
                    }
                }
            });

        } else {

            Log.i("swip", "disabled");
        }
        if (isTabsEnabled) {
            tabsView = new FrameLayout(context);
            initTabs(context);
            frameLayout.addView(tabsView, LayoutHelper.createFrame(-1, (float) tabsHeight, tabsToBottom ? 80 : 48, 0.0f, 0.0f, 0.0f, 0.0f));
        }


        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING && searching && searchWas) {
                    AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                int visibleItemCount = Math.abs(layoutManager.findLastVisibleItemPosition() - firstVisibleItem) + 1;
                int totalItemCount = recyclerView.getAdapter().getItemCount();

                if (searching && searchWas) {
                    if (visibleItemCount > 0 && layoutManager.findLastVisibleItemPosition() == totalItemCount - 1 && !dialogsSearchAdapter.isMessagesSearchEndReached()) {
                        dialogsSearchAdapter.loadMoreSearchMessages();
                    }
                    return;
                }
                if (visibleItemCount > 0) {
                    if (layoutManager.findLastVisibleItemPosition() >= getDialogsArray().size() - 10) {
                        boolean fromCache = !MessagesController.getInstance().dialogsEndReached;
                        if (fromCache || !MessagesController.getInstance().serverDialogsEndReached) {
                            MessagesController.getInstance().loadDialogs(-1, 100, fromCache);
                        }
                    }
                }

                if (floatingButton.getVisibility() != View.GONE) {
                    final View topChild = recyclerView.getChildAt(0);
                    int firstViewTop = 0;
                    if (topChild != null) {
                        firstViewTop = topChild.getTop();
                    }
                    boolean goingDown;
                    boolean changed = true;
                    if (prevPosition == firstVisibleItem) {
                        final int topDelta = prevTop - firstViewTop;
                        goingDown = firstViewTop < prevTop;
                        changed = Math.abs(topDelta) > 1;
                    } else {
                        goingDown = firstVisibleItem > prevPosition;
                    }
                    if (changed && scrollUpdated) {
                        hideFloatingButton(goingDown);
                    }
                    prevPosition = firstVisibleItem;
                    prevTop = firstViewTop;
                    scrollUpdated = true;
                }


                if (!hideTabs) {
                    if (dy > 1 && recyclerView.getChildAt(0).getTop() < 0) {
                        if (disableAnimation) {
                            hideFloatingButton(true);
                        } else {
                            hideTabsAnimated(true);
                        }
                    }
                    if (dy >= -1) {
                        return;
                    }
                    if (disableAnimation) {
                        hideFloatingButton(false);
                        return;
                    }
                    hideTabsAnimated(false);
                    if (firstVisibleItem == 0) {
                        boolean tabsToBottom = preferences.getBoolean("tabsToBottom", false);

                        listView.setPadding(0, tabsToBottom ? 0 : AndroidUtilities.dp((float) tabsHeight), 0, tabsToBottom ? AndroidUtilities.dp((float) tabsHeight) : 0);
                    }

                } else if (visibleItemCount > 0 && layoutManager.findLastVisibleItemPosition() == totalItemCount - 1 && !dialogsSearchAdapter.isMessagesSearchEndReached()) {
                    dialogsSearchAdapter.loadMoreSearchMessages();
                }
            }
        });

        if (searchString == null) {
            dialogsAdapter = new DialogsAdapter(context, dialogsType);
            if (AndroidUtilities.isTablet() && openedDialogId != 0) {
                dialogsAdapter.setOpenedDialogId(openedDialogId);
            }
            listView.setAdapter(dialogsAdapter);
        }
        int type = 0;
        if (searchString != null) {
            type = 2;
        } else if (!onlySelect) {
            type = 1;
        }
        dialogsSearchAdapter = new DialogsSearchAdapter(context, type, dialogsType);
        dialogsSearchAdapter.setDelegate(new DialogsSearchAdapter.DialogsSearchAdapterDelegate() {
            @Override
            public void searchStateChanged(boolean search) {
                if (searching && searchWas && searchEmptyView != null) {
                    if (search) {
                        searchEmptyView.showProgress();
                    } else {
                        searchEmptyView.showTextView();
                    }
                }
            }

            @Override
            public void didPressedOnSubDialog(int did) {
                if (onlySelect) {
                    didSelectResult(did, true, false);
                } else {
                    Bundle args = new Bundle();
                    if (did > 0) {
                        args.putInt("user_id", did);
                    } else {
                        args.putInt("chat_id", -did);
                    }
                    if (actionBar != null) {
                        actionBar.closeSearchField();
                    }
                    if (AndroidUtilities.isTablet()) {
                        if (dialogsAdapter != null) {
                            dialogsAdapter.setOpenedDialogId(openedDialogId = did);
                            updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
                        }
                    }
                    if (searchString != null) {
                        if (MessagesController.checkCanOpenChat(args, DialogsActivity.this)) {
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.closeChats);
                            presentFragment(new ChatActivity(args));
                        }
                    } else {
                        if (MessagesController.checkCanOpenChat(args, DialogsActivity.this)) {
                            presentFragment(new ChatActivity(args));
                        }
                    }
                }
            }

            @Override
            public void needRemoveHint(final int did) {
                if (getParentActivity() == null) {
                    return;
                }
                TLRPC.User user = MessagesController.getInstance().getUser(did);
                if (user == null) {
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                builder.setMessage(LocaleController.formatString("ChatHintsDelete", R.string.ChatHintsDelete, ContactsController.formatName(user.first_name, user.last_name)));
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SearchQuery.removePeer(did);
                    }
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                showDialog(builder.create());
            }
        });

        if (MessagesController.getInstance().loadingDialogs && MessagesController.getInstance().dialogs.isEmpty()) {
            searchEmptyView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            listView.setEmptyView(progressView);
        } else {
            searchEmptyView.setVisibility(View.GONE);
            progressView.setVisibility(View.GONE);
            listView.setEmptyView(emptyView);
        }
        if (searchString != null) {
            actionBar.openSearchField(searchString);
        }

        if (!onlySelect && dialogsType == 0) {
            frameLayout.addView(fragmentContextView = new FragmentContextView(context, this), LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 39, Gravity.TOP | Gravity.LEFT, 0, -36, 0, 0));
        }

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);

        if (dialogsAdapter != null) {
            dialogsAdapter.notifyDataSetChanged();
        }
        if (dialogsSearchAdapter != null) {
            dialogsSearchAdapter.notifyDataSetChanged();
        }
        if (checkPermission && !onlySelect && Build.VERSION.SDK_INT >= 23) {
            Activity activity = getParentActivity();
            if (activity != null) {
                checkPermission = false;
                if (activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED || activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setMessage(LocaleController.getString("PermissionContacts", R.string.PermissionContacts));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                        showDialog(permissionDialog = builder.create());
                    } else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                        builder.setMessage(LocaleController.getString("PermissionStorage", R.string.PermissionStorage));
                        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                        showDialog(permissionDialog = builder.create());
                    } else {
                        askForPermissons();
                    }
                }
            }
        }
        refreshTabs();
        paintHeader(true);
        unreadCount();

    }

    @TargetApi(Build.VERSION_CODES.M)
    private void askForPermissons() {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        ArrayList<String> permissons = new ArrayList<>();
        if (activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissons.add(Manifest.permission.READ_CONTACTS);
            permissons.add(Manifest.permission.WRITE_CONTACTS);
            permissons.add(Manifest.permission.GET_ACCOUNTS);
        }
        if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissons.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissons.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        String[] items = permissons.toArray(new String[permissons.size()]);
        activity.requestPermissions(items, 1);
    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        super.onDialogDismiss(dialog);
        if (permissionDialog != null && dialog == permissionDialog && getParentActivity() != null) {
            askForPermissons();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        final boolean tabsToBottom = preferences.getBoolean("tabsToBottom", false);
        if (!onlySelect && floatingButton != null) {

            floatingButton.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    int i = 100;
                    if (tabsToBottom) {
                        i = 150;
                    } else {
                        i = 100;
                    }
                    floatingButton.setTranslationY(floatingHidden ? AndroidUtilities.dp(i) : 0);
                    floatingButton.setClickable(!floatingHidden);
                    if (floatingButton != null) {
                        if (Build.VERSION.SDK_INT < 16) {
                            floatingButton.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        } else {
                            floatingButton.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResultFragment(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            for (int a = 0; a < permissions.length; a++) {
                if (grantResults.length <= a || grantResults[a] != PackageManager.PERMISSION_GRANTED) {
                    continue;
                }
                switch (permissions[a]) {
                    case Manifest.permission.READ_CONTACTS:
                        ContactsController.getInstance().readContacts();
                        break;
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        ImageLoader.getInstance().checkMediaPaths();
                        break;
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.dialogsNeedReload) {
            if (dialogsAdapter != null) {
                if (dialogsAdapter.isDataSetChanged()) {
                    dialogsAdapter.notifyDataSetChanged();
                } else {
                    updateVisibleRows(MessagesController.UPDATE_MASK_NEW_MESSAGE);
                }
            }
            if (dialogsSearchAdapter != null) {
                dialogsSearchAdapter.notifyDataSetChanged();
            }
            if (listView != null) {
                try {
                    if (MessagesController.getInstance().loadingDialogs && MessagesController.getInstance().dialogs.isEmpty()) {
                        searchEmptyView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.GONE);
                        listView.setEmptyView(progressView);
                    } else {
                        progressView.setVisibility(View.GONE);
                        if (searching && searchWas) {
                            emptyView.setVisibility(View.GONE);
                            listView.setEmptyView(searchEmptyView);
                        } else {
                            searchEmptyView.setVisibility(View.GONE);
                            listView.setEmptyView(emptyView);
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e); //TODO fix it in other way?
                }
            }
        } else if (id == NotificationCenter.emojiDidLoaded) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.updateInterfaces) {
            updateVisibleRows((Integer) args[0]);
        } else if (id == NotificationCenter.appDidLogout) {
            dialogsLoaded = false;
        } else if (id == NotificationCenter.encryptedChatUpdated) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.contactsDidLoaded) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.openedChatChanged) {
            if (dialogsType == 0 && AndroidUtilities.isTablet()) {
                boolean close = (Boolean) args[1];
                long dialog_id = (Long) args[0];
                if (close) {
                    if (dialog_id == openedDialogId) {
                        openedDialogId = 0;
                    }
                } else {
                    openedDialogId = dialog_id;
                }
                if (dialogsAdapter != null) {
                    dialogsAdapter.setOpenedDialogId(openedDialogId);
                }
                updateVisibleRows(MessagesController.UPDATE_MASK_SELECT_DIALOG);
            }
        } else if (id == NotificationCenter.notificationsSettingsUpdated) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.messageReceivedByAck || id == NotificationCenter.messageReceivedByServer || id == NotificationCenter.messageSendError) {
            updateVisibleRows(MessagesController.UPDATE_MASK_SEND_STATE);
        } else if (id == NotificationCenter.didSetPasscode) {
            updatePasscodeButton();
        } else if (id == NotificationCenter.needReloadRecentDialogsSearch) {
            if (dialogsSearchAdapter != null) {
                dialogsSearchAdapter.loadRecentSearch();
            }
        }else if (id == NotificationCenter.refreshTabs) {
            int i = (int) args[0];
            if (i == 14 || i == 12) {
                tabsHeight = preferences.getInt("tabsHeight", 40);
                if (tabsView != null) {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) tabsView.getLayoutParams();
                    params.gravity = preferences.getBoolean("tabsToBottom", false) ? 80 : 48;
                    tabsView.setLayoutParams(params);
                }
                if (floatingButton != null) {
                    FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) floatingButton.getLayoutParams();
                    layoutParams.bottomMargin = AndroidUtilities.dp(preferences.getBoolean("tabsToBottom", false) ? (float) (tabsHeight + 14) : 14.0f);
                    floatingButton.setLayoutParams(layoutParams);
                }
                if (i == 14) {
                    changedTabPosition = true;
                }
            }
            updateTabs();
            hideShowTabs((int) args[0]);

        } else if (id == NotificationCenter.didLoadedReplyMessages) {
            updateVisibleRows(0);
        } else if (id == NotificationCenter.reloadHints) {
            if (dialogsSearchAdapter != null) {
                dialogsSearchAdapter.notifyDataSetChanged();
            }
        }
    }
    private void hideShowTabs(int i) {
        RelativeLayout rl = null;
        int pos = 0;
        boolean b = false;
        SharedPreferences plusPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        boolean hideUsers = plusPreferences.getBoolean("hideUsers", false);
        boolean hideGroups = plusPreferences.getBoolean("hideGroups", false);
        boolean hideSGroups = plusPreferences.getBoolean("hideSGroups", false);
        boolean hideBots = plusPreferences.getBoolean("hideBots", false);
        boolean hideFavs = plusPreferences.getBoolean("hideFavs", false);
        switch (i) {
            case 0: // Users
                rl = (RelativeLayout) usersTab.getParent();
                pos = 1;
                b = hideUsers;
                break;
            case 1: //Groups
                rl = (RelativeLayout) groupsTab.getParent();
                pos = hideUsers ? 1 : 2;
                b = hideGroups;
                break;
            case 2: //Supergroups
                rl = (RelativeLayout) superGroupsTab.getParent();
                pos = 3;
                if (hideGroups) pos = pos - 1;
                if (hideUsers) pos = pos - 1;
                b = hideSGroups;
                break;
            case 3: //Channels
                rl = (RelativeLayout) channelsTab.getParent();
                pos = tabsLayout.getChildCount();
                if (!hideBots) pos = pos - 1;
                if (!hideFavs) pos = pos - 1;
                b = plusPreferences.getBoolean("hideChannels", false);
                break;
            case 4: //Bots
                rl = (RelativeLayout) botsTab.getParent();
                pos = tabsLayout.getChildCount();
                if (!hideFavs) pos = pos - 1;
                b = hideBots;
                break;
            case 5: //Favorites
                rl = (RelativeLayout) favsTab.getParent();
                pos = tabsLayout.getChildCount();
                b = hideFavs;
                break;
            default:
                updateTabs();
        }

        if (rl != null) {
            if (!b) {
                tabsLayout.addView(rl, pos, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f));
            } else {
                tabsLayout.removeView(rl);
            }
        }
    }

    private ArrayList<TLRPC.TL_dialog> getDialogsArray() {
        if (dialogsType == 0) {
            return MessagesController.getInstance().dialogs;
        } else if (dialogsType == 1) {
            return MessagesController.getInstance().dialogsServerOnly;
        } else if (dialogsType == 2) {
            return MessagesController.getInstance().dialogsGroupsOnly;
        } else if (dialogsType == 3) {
            return MessagesController.getInstance().dialogsChannels;
        } else if (dialogsType == 4) {
            return MessagesController.getInstance().dialogsGroups;
        } else if (dialogsType == 5) {
            return MessagesController.getInstance().dialogsBots;
        } else if (dialogsType == 6) {
            return MessagesController.getInstance().dialogsUsers;
        } else if (dialogsType == 7) {
            return MessagesController.getInstance().dialogsMegaGroups;
        } else if (dialogsType == 8) {
            return MessagesController.getInstance().dialogsFavs;
        } else if (dialogsType == 9) {
            return MessagesController.getInstance().dialogsGroupsAll;
        }

        return null;
    }

    public void setSideMenu(RecyclerView recyclerView) {
        sideMenu = recyclerView;
        sideMenu.setBackgroundColor(Theme.getColor(Theme.key_chats_menuBackground));
        sideMenu.setGlowColor(Theme.getColor(Theme.key_chats_menuBackground));
    }

    private void updatePasscodeButton() {
        if (passcodeItem == null) {
            return;
        }
        if (UserConfig.passcodeHash.length() != 0 && !searching) {
            passcodeItem.setVisibility(View.VISIBLE);
            if (UserConfig.appLocked) {
                passcodeItem.setIcon(R.drawable.lock_close);
            } else {
                passcodeItem.setIcon(R.drawable.lock_open);
            }
        } else {
            passcodeItem.setVisibility(View.GONE);
        }
    }

    private void hideFloatingButton(boolean hide) {
        if (floatingHidden == hide) {
            return;
        }
        int i = 0;
        boolean tabsToBottom = preferences.getBoolean("tabsToBottom", false);
        floatingHidden = hide;
        if (tabsToBottom) {
            i = 150;
        } else {
            i = 100;

        }
        ObjectAnimator animator = ObjectAnimator.ofFloat(floatingButton, "translationY", floatingHidden ? AndroidUtilities.dp(i) : 0).setDuration(300);
        animator.setInterpolator(floatingInterpolator);
        floatingButton.setClickable(!hide);
        animator.start();
    }

    private void updateVisibleRows(int mask) {
        if (listView == null) {
            return;
        }
        int count = listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View child = listView.getChildAt(a);
            if (child instanceof DialogCell) {
                if (listView.getAdapter() != dialogsSearchAdapter) {
                    DialogCell cell = (DialogCell) child;
                    if ((mask & MessagesController.UPDATE_MASK_NEW_MESSAGE) != 0) {
                        cell.checkCurrentDialogIndex();
                        if (dialogsType == 0 && AndroidUtilities.isTablet()) {
                            cell.setDialogSelected(cell.getDialogId() == openedDialogId);
                        }
                    } else if ((mask & MessagesController.UPDATE_MASK_SELECT_DIALOG) != 0) {
                        if (dialogsType == 0 && AndroidUtilities.isTablet()) {
                            cell.setDialogSelected(cell.getDialogId() == openedDialogId);
                        }
                    } else {
                        cell.update(mask);
                    }
                }
            } else if (child instanceof UserCell) {
                ((UserCell) child).update(mask);
            } else if (child instanceof ProfileSearchCell) {
                ((ProfileSearchCell) child).update(mask);
            } else if (child instanceof RecyclerListView) {
                RecyclerListView innerListView = (RecyclerListView) child;
                int count2 = innerListView.getChildCount();
                for (int b = 0; b < count2; b++) {
                    View child2 = innerListView.getChildAt(b);
                    if (child2 instanceof HintDialogCell) {
                        ((HintDialogCell) child2).checkUnreadCounter(mask);
                    }
                }
            }
        }
        unreadCount();
    }

    public void setDelegate(DialogsActivityDelegate dialogsActivityDelegate) {
        delegate = dialogsActivityDelegate;
    }

    public void setSearchString(String string) {
        searchString = string;
    }

    public boolean isMainDialogList() {
        return delegate == null && searchString == null;
    }

    private void didSelectResult(final long dialog_id, boolean useAlert, final boolean param) {
        if (addToGroupAlertString == null) {
            if ((int) dialog_id < 0) {
                TLRPC.Chat chat = MessagesController.getInstance().getChat(-(int) dialog_id);
                if (ChatObject.isChannel(chat) && !chat.megagroup && (cantSendToChannels || !ChatObject.isCanWriteToChannel(-(int) dialog_id))) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                    builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
                    builder.setMessage(LocaleController.getString("ChannelCantSendMessage", R.string.ChannelCantSendMessage));
                    builder.setNegativeButton(LocaleController.getString("OK", R.string.OK), null);
                    showDialog(builder.create());
                    return;
                }
            }
        }
        if (useAlert && (selectAlertString != null && selectAlertStringGroup != null || addToGroupAlertString != null)) {
            if (getParentActivity() == null) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
            builder.setTitle(LocaleController.getString("AppName", R.string.AppName));
            int lower_part = (int) dialog_id;
            int high_id = (int) (dialog_id >> 32);
            if (lower_part != 0) {
                if (high_id == 1) {
                    TLRPC.Chat chat = MessagesController.getInstance().getChat(lower_part);
                    if (chat == null) {
                        return;
                    }
                    builder.setMessage(LocaleController.formatStringSimple(selectAlertStringGroup, chat.title));
                } else {
                    if (lower_part > 0) {
                        TLRPC.User user = MessagesController.getInstance().getUser(lower_part);
                        if (user == null) {
                            return;
                        }
                        builder.setMessage(LocaleController.formatStringSimple(selectAlertString, UserObject.getUserName(user)));
                    } else if (lower_part < 0) {
                        TLRPC.Chat chat = MessagesController.getInstance().getChat(-lower_part);
                        if (chat == null) {
                            return;
                        }
                        if (addToGroupAlertString != null) {
                            builder.setMessage(LocaleController.formatStringSimple(addToGroupAlertString, chat.title));
                        } else {
                            builder.setMessage(LocaleController.formatStringSimple(selectAlertStringGroup, chat.title));
                        }
                    }
                }
            } else {
                TLRPC.EncryptedChat chat = MessagesController.getInstance().getEncryptedChat(high_id);
                TLRPC.User user = MessagesController.getInstance().getUser(chat.user_id);
                if (user == null) {
                    return;
                }
                builder.setMessage(LocaleController.formatStringSimple(selectAlertString, UserObject.getUserName(user)));
            }

            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    didSelectResult(dialog_id, false, false);
                }
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            showDialog(builder.create());
        } else {
            if (delegate != null) {
                delegate.didSelectDialog(DialogsActivity.this, dialog_id, param);
                delegate = null;
            } else {
                finishFragment();
            }
        }
    }

    @Override
    public ThemeDescription[] getThemeDescriptions() {
        ThemeDescription.ThemeDescriptionDelegate сellDelegate = new ThemeDescription.ThemeDescriptionDelegate() {
            @Override
            public void didSetColor(int color) {
                int count = listView.getChildCount();
                for (int a = 0; a < count; a++) {
                    View child = listView.getChildAt(a);
                    if (child instanceof ProfileSearchCell) {
                        ((ProfileSearchCell) child).update(0);
                    } else if (child instanceof DialogCell) {
                        ((DialogCell) child).update(0);
                    }
                }
                RecyclerListView recyclerListView = dialogsSearchAdapter.getInnerListView();
                if (recyclerListView != null) {
                    count = recyclerListView.getChildCount();
                    for (int a = 0; a < count; a++) {
                        View child = recyclerListView.getChildAt(a);
                        if (child instanceof HintDialogCell) {
                            ((HintDialogCell) child).update();
                        }
                    }
                }
            }
        };
        return new ThemeDescription[]{
                new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite),

                new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault),
                new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCH, null, null, null, null, Theme.key_actionBarDefaultSearch),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCHPLACEHOLDER, null, null, null, null, Theme.key_actionBarDefaultSearchPlaceholder),

                new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector),

                new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider),

                new ThemeDescription(searchEmptyView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_emptyListPlaceholder),
                new ThemeDescription(searchEmptyView, ThemeDescription.FLAG_PROGRESSBAR, null, null, null, null, Theme.key_progressCircle),

                new ThemeDescription(emptyTextView1, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_emptyListPlaceholder),
                new ThemeDescription(emptyTextView2, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_emptyListPlaceholder),

                new ThemeDescription(floatingButton, ThemeDescription.FLAG_IMAGECOLOR, null, null, null, null, Theme.key_chats_actionIcon),
                new ThemeDescription(floatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER, null, null, null, null, Theme.key_chats_actionBackground),
                new ThemeDescription(floatingButton, ThemeDescription.FLAG_BACKGROUNDFILTER | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, null, null, null, null, Theme.key_chats_actionPressedBackground),

                new ThemeDescription(listView, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.avatar_photoDrawable, Theme.avatar_broadcastDrawable}, null, Theme.key_avatar_text),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundRed),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundOrange),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundViolet),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundGreen),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundCyan),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundBlue),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_avatar_backgroundPink),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_countPaint, null, null, Theme.key_chats_unreadCounter),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_countGrayPaint, null, null, Theme.key_chats_unreadCounterMuted),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_countTextPaint, null, null, Theme.key_chats_unreadCounterText),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, Theme.dialogs_namePaint, null, null, Theme.key_chats_name),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, Theme.dialogs_nameEncryptedPaint, null, null, Theme.key_chats_secretName),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_lockDrawable}, null, Theme.key_chats_secretIcon),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_groupDrawable, Theme.dialogs_broadcastDrawable, Theme.dialogs_botDrawable}, null, Theme.key_chats_nameIcon),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_pinnedDrawable}, null, Theme.key_chats_pinnedIcon),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_messagePaint, null, null, Theme.key_chats_message),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_chats_nameMessage),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_chats_draft),
                new ThemeDescription(null, 0, null, null, null, сellDelegate, Theme.key_chats_attachMessage),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_messagePrintingPaint, null, null, Theme.key_chats_actionMessage),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_timePaint, null, null, Theme.key_chats_date),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_pinnedPaint, null, null, Theme.key_chats_pinnedOverlay),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_tabletSeletedPaint, null, null, Theme.key_chats_tabletSelectedOverlay),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_checkDrawable, Theme.dialogs_halfCheckDrawable}, null, Theme.key_chats_sentCheck),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_clockDrawable}, null, Theme.key_chats_sentClock),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, Theme.dialogs_errorPaint, null, null, Theme.key_chats_sentError),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_errorDrawable}, null, Theme.key_chats_sentErrorIcon),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_verifiedCheckDrawable}, null, Theme.key_chats_verifiedCheck),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class, ProfileSearchCell.class}, null, new Drawable[]{Theme.dialogs_verifiedDrawable}, null, Theme.key_chats_verifiedBackground),
                new ThemeDescription(listView, 0, new Class[]{DialogCell.class}, null, new Drawable[]{Theme.dialogs_muteDrawable}, null, Theme.key_chats_muteIcon),

                new ThemeDescription(sideMenu, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_chats_menuBackground),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuName),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuPhone),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuPhoneCats),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuCloudBackgroundCats),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, new String[]{"cloudDrawable"}, null, null, null, Theme.key_chats_menuCloud),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chat_serviceBackground),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuTopShadow),

                new ThemeDescription(sideMenu, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{DrawerActionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_chats_menuItemIcon),
                new ThemeDescription(sideMenu, 0, new Class[]{DrawerActionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_chats_menuItemText),

                new ThemeDescription(sideMenu, 0, new Class[]{DividerCell.class}, Theme.dividerPaint, null, null, Theme.key_divider),

                new ThemeDescription(listView, 0, new Class[]{LoadingCell.class}, new String[]{"progressBar"}, null, null, null, Theme.key_progressCircle),

                new ThemeDescription(listView, 0, new Class[]{ProfileSearchCell.class}, Theme.dialogs_offlinePaint, null, null, Theme.key_windowBackgroundWhiteGrayText3),
                new ThemeDescription(listView, 0, new Class[]{ProfileSearchCell.class}, Theme.dialogs_onlinePaint, null, null, Theme.key_windowBackgroundWhiteBlueText3),

                new ThemeDescription(listView, 0, new Class[]{GraySectionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2),
                new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{GraySectionCell.class}, null, null, null, Theme.key_graySection),

                new ThemeDescription(listView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{HashtagSearchCell.class}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),

                new ThemeDescription(progressView, ThemeDescription.FLAG_PROGRESSBAR, null, null, null, null, Theme.key_progressCircle),

                new ThemeDescription(dialogsSearchAdapter.getInnerListView(), 0, new Class[]{HintDialogCell.class}, Theme.dialogs_countPaint, null, null, Theme.key_chats_unreadCounter),
                new ThemeDescription(dialogsSearchAdapter.getInnerListView(), 0, new Class[]{HintDialogCell.class}, Theme.dialogs_countGrayPaint, null, null, Theme.key_chats_unreadCounterMuted),
                new ThemeDescription(dialogsSearchAdapter.getInnerListView(), 0, new Class[]{HintDialogCell.class}, Theme.dialogs_countTextPaint, null, null, Theme.key_chats_unreadCounterText),
                new ThemeDescription(dialogsSearchAdapter.getInnerListView(), 0, new Class[]{HintDialogCell.class}, new String[]{"nameTextView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),

                new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{FragmentContextView.class}, new String[]{"frameLayout"}, null, null, null, Theme.key_inappPlayerBackground),
                new ThemeDescription(fragmentContextView, 0, new Class[]{FragmentContextView.class}, new String[]{"playButton"}, null, null, null, Theme.key_inappPlayerPlayPause),
                new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{FragmentContextView.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_inappPlayerTitle),
                new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_TEXTCOLOR, new Class[]{FragmentContextView.class}, new String[]{"frameLayout"}, null, null, null, Theme.key_inappPlayerPerformer),
                new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{FragmentContextView.class}, new String[]{"closeButton"}, null, null, null, Theme.key_inappPlayerClose),

                new ThemeDescription(fragmentContextView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{FragmentContextView.class}, new String[]{"frameLayout"}, null, null, null, Theme.key_returnToCallBackground),
                new ThemeDescription(fragmentContextView, 0, new Class[]{FragmentContextView.class}, new String[]{"titleTextView"}, null, null, null, Theme.key_returnToCallText),

                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogBackground),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlack),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextLink),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogLinkSelection),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue2),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue3),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextBlue4),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextRed),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray2),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray3),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextGray4),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogIcon),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogTextHint),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogInputField),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogInputFieldActivated),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareBackground),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareCheck),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareUnchecked),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogCheckboxSquareDisabled),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRadioBackground),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRadioBackgroundChecked),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogProgressCircle),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogButton),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogButtonSelector),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogScrollGlow),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRoundCheckBox),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogRoundCheckBoxCheck),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogBadgeBackground),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogBadgeText),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogLineProgress),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogLineProgressBackground),
                new ThemeDescription(null, 0, null, null, null, null, Theme.key_dialogGrayLine),
        };
    }

    private void refreshDialogType(int d) {
        //  Toast.makeText(ApplicationLoader.applicationContext, ""+d, Toast.LENGTH_SHORT).show();
        if (!hideTabs) {
            boolean hideUsers = preferences.getBoolean("hideUsers", false);
            boolean hideGroups = preferences.getBoolean("hideGroups", false);
            boolean hideSGroups = preferences.getBoolean("hideSGroups", false);
            boolean hideChannels = preferences.getBoolean("hideChannels", false);
            boolean hideBots = preferences.getBoolean("hideBots", false);
            boolean hideFavs = preferences.getBoolean("hideFavs", false);
            boolean loop = preferences.getBoolean("infiniteTabsSwipe", true);
            if (d == 1) {
                switch (dialogsType) {
                    case 6:
                        dialogsType = !hideGroups ? 4 : !hideSGroups ? 7 : !hideChannels ? 3 : !hideBots ? 5 : !hideFavs ? 8 : loop ? 0 : dialogsType;

                        break;
                    case 4:
                        dialogsType = !hideSGroups ? 7 : !hideChannels ? 3 : !hideBots ? 5 : !hideFavs ? 8 : loop ? 0 : dialogsType;
                        break;
                    case 3:
                        dialogsType = !hideBots ? 5 : !hideFavs ? 8 : loop ? 0 : dialogsType;
                        break;
                    case 5:
                        dialogsType = !hideFavs ? 8 : loop ? 0 : dialogsType;
                        break;
                    case 7:
                        dialogsType = !hideChannels ? 3 : !hideBots ? 5 : !hideFavs ? 8 : loop ? 0 : dialogsType;
                        break;
                    case 8:
                        if (loop) {
                            dialogsType = 0;
                            break;
                        }
                        break;
                    default:
                        dialogsType = !hideUsers ? 6 : (hideGroups || !hideSGroups) ? !hideGroups ? 4 : !hideChannels ? 3 : !hideBots ? 5 : !hideFavs ? 8 : loop ? 0 : dialogsType : 0;
                        break;
                }
            }
            if (d == 0) {
                switch (dialogsType) {
                    case 6:
                        dialogsType = 0;
                        break;
                    case 4:
                        dialogsType = !hideUsers ? 6 : 0;
                        break;
                    case 3:
                        dialogsType = !hideSGroups ? 7 : !hideGroups ? 4 : !hideUsers ? 6 : 0;
                        break;
                    case 5:
                        dialogsType = !hideChannels ? 3 : !hideSGroups ? 7 : !hideGroups ? 4 : !hideUsers ? 6 : 0;
                        break;
                    case 7:
                        dialogsType = !hideGroups ? 4 : !hideUsers ? 6 : 0;
                        break;
                    case 8:
                        dialogsType = !hideBots ? 5 : !hideChannels ? 3 : !hideSGroups ? 7 : !hideGroups ? 4 : !hideUsers ? 6 : 0;
                        break;
                    default:
                        if (loop) {
                            dialogsType = !hideFavs ? 8 : !hideBots ? 5 : !hideChannels ? 3 : !hideSGroups ? 7 : !hideGroups ? 4 : !hideUsers ? 6 : 0;
                        }
                }
            }
        }
    }

}
