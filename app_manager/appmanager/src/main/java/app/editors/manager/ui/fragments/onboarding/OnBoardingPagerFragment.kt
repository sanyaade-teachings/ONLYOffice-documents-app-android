package app.editors.manager.ui.fragments.onboarding

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import app.editors.manager.R
import app.editors.manager.app.App
import app.editors.manager.databinding.FragmentOnBoardingPagerBinding
import app.editors.manager.managers.tools.PreferenceTool
import app.editors.manager.managers.utils.isVisible
import app.editors.manager.ui.fragments.base.BaseAppFragment
import app.editors.manager.ui.views.pager.ViewPagerAdapter
import com.rd.animation.type.AnimationType
import lib.toolkit.base.managers.utils.SwipeEventUtils
import javax.inject.Inject

class OnBoardingPagerFragment : BaseAppFragment() {

    @Inject
    lateinit var preferenceTool: PreferenceTool

    private var mOnBoardAdapter: OnBoardAdapter? = null
    private var viewBinding: FragmentOnBoardingPagerBinding? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        App.getApp().appComponent.inject(this)
    }

    override fun dispatchTouchEvent(ev: MotionEvent) { }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewBinding = FragmentOnBoardingPagerBinding.inflate(inflater, container, false)
        return viewBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }

    override fun onBackPressed(): Boolean {
        minimizeApp()
        return true
    }

    private fun finishWithOkCode() {
        requireActivity().setResult(Activity.RESULT_OK)
        requireActivity().finish()
    }

    private fun init() {
        viewBinding?.let { binding ->
            mOnBoardAdapter = OnBoardAdapter(childFragmentManager, fragments)
            binding.onBoardingViewPager.adapter = mOnBoardAdapter
            binding.onBoardingViewPager.addOnPageChangeListener(mOnBoardAdapter!!)
            binding.include.onBoardingPanelIndicator.setAnimationType(AnimationType.WORM)
            binding.include.onBoardingPanelIndicator.setViewPager(binding.onBoardingViewPager)
            binding.include.onBoardingPanelSkipButton.setOnClickListener {
                preferenceTool.onBoarding = true
                finishWithOkCode()
            }
            binding.include.onBoardingPanelNextButton.setOnClickListener {
                mOnBoardAdapter?.isLastPagePosition?.let {
                    finishWithOkCode()
                } ?: run {
                    binding.onBoardingViewPager
                        .setCurrentItem(mOnBoardAdapter?.selectedPage!! + 1, true)
                }
            }
        }
    }

    private fun getInstance(screen: Int) =
        ViewPagerAdapter.Container(OnBoardingPageFragment.newInstance(
            R.string.on_boarding_welcome_header,
            R.string.on_boarding_welcome_info, screen), null)

    private val fragments: List<ViewPagerAdapter.Container?>
        get() = listOf(
            getInstance(R.drawable.image_on_boarding_screen1),
            getInstance(R.drawable.image_on_boarding_screen2),
            getInstance(R.drawable.image_on_boarding_screen3),
            getInstance(R.drawable.image_on_boarding_screen4),
            getInstance(R.drawable.image_on_boarding_screen5))

    /*
     * Pager adapter
     * */
    private inner class OnBoardAdapter(manager: FragmentManager?, fragmentList: List<Container?>?) :
        ViewPagerAdapter(manager, fragmentList) {
        private var position = 0

        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            viewBinding?.let {
                if (position == mOnBoardAdapter?.count!! - 1) {
                    it.include.onBoardingPanelNextButton.setText(R.string.on_boarding_finish_button)
                    it.include.onBoardingPanelSkipButton.isVisible = true
                    preferenceTool.onBoarding = true
                } else {
                    it.include.onBoardingPanelNextButton.setText(R.string.on_boarding_next_button)
                    it.include.onBoardingPanelSkipButton.isVisible = false
                }
            }
            this.position = position
        }

        init {
            SwipeEventUtils.detectLeft(viewBinding?.onBoardingViewPager!!,
                object : SwipeEventUtils.SwipeSingleCallback {
                override fun onSwipe() {
                    if (position == mOnBoardAdapter?.count!! - 1) {
                        viewBinding?.include?.onBoardingPanelNextButton?.callOnClick()
                    }
                }
            })
        }
    }

    companion object {
        val TAG = OnBoardingPagerFragment::class.java.simpleName

        fun newInstance(): OnBoardingPagerFragment {
            return OnBoardingPagerFragment()
        }
    }
}