package com.truckintransit.operator.fragment.vehicleEdit


import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.app.Fragment
import android.content.Intent
import android.view.*
import android.widget.DatePicker
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView

import com.truckintransit.operator.R
import com.truckintransit.operator.base.BaseApplication
import com.truckintransit.operator.base.BaseFragment
import com.truckintransit.operator.callbackInterface.ListnerForDialog
import com.truckintransit.operator.constants.AppConstants
import com.truckintransit.operator.dataprefence.DataManager
import com.truckintransit.operator.newtworking.ApiRequestClient
import com.truckintransit.operator.pojo.ResponseFromServerGeneric
import com.truckintransit.operator.pojo.vehicleDetailList.ResponseFromServerVehicleDetailList
import com.truckintransit.operator.utils.FileHelper
import com.truckintransit.operator.utils.UtiliyMethods
import com.truckintransit.operator.utils.Validation
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_add_vehicle_rc.view.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class AddVehicleRcEditFragment : BaseFragment(), ListnerForDialog {
    private lateinit var mPermitValidDate: String
    private lateinit var et_valid_upto: TextInputEditText
    var cal = Calendar.getInstance()
    private lateinit var et_rc_no: TextInputEditText
    private lateinit var mNumberRc: String
    private var mCompositeDisposable_update_Photo: CompositeDisposable? = null
    private lateinit var dataManager: DataManager
    private lateinit var iv_profile: ImageView
    private  var IsprofilePhoto:Boolean?=false
    private lateinit var mFileUserPhoto: File

    override fun selctok() {
        activity!!.supportFragmentManager.popBackStack()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view:View=  inflater.inflate(R.layout.fragment_add_vehicle_rc, container, false)
        view.tv_rc.text=getString(R.string.edit_rc)
        view.tv_attach_photo_rc.text=getString(R.string.edit_rc_photo)
        et_rc_no=view.et_rc_no
        iv_profile=view.iv_profile
        dataManager = BaseApplication.baseApplicationInstance.getdatamanger()
        et_valid_upto=view.et_valid_upto

        view.relative_profile.setOnClickListener {
            startPic(4, 4)
        }
        view.save.setOnClickListener {
            if(!validationForAddDriverAdaharPhoto()){
                uploadingUserPhoto()


            }


        }
        startLoadingForDriverDetails()

        // create an OnDateSetListener
        val dateSetListener = object : DatePickerDialog.OnDateSetListener {
            override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int,
                                   dayOfMonth: Int) {
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            }
        }
        et_valid_upto.setOnClickListener {
            DatePickerDialog(activity!!, R.style.MyDialogTheme,
                dateSetListener,
                // set DatePickerDialog to point to today's date when it loads up
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show()


        }
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // inflater.inflate(R.menu.adddriver, menu)

    }

    private fun startPic(aspectRatioX: Int, aspectRatioY: Int) {
        CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .setActivityTitle("Crop")

            .setCropMenuCropButtonTitle("Done")
            .setCropMenuCropButtonIcon(R.drawable.ic_green_right_rounded)

            .start(context!!, this)
    }

    private fun onImageResult(file: File) {
        IsprofilePhoto=true
        Glide.with(this).load(file).into(iv_profile)
        mFileUserPhoto = file
        editingUserPhoto()

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                val resultUri = result.uri

                onImageResult(File(FileHelper.getPath(activity!!, resultUri)))
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
            }
        }
    }
    // api call for user registration
    private fun uploadingUserPhoto() {

        showDialogLoading()


        mCompositeDisposable_update_Photo = CompositeDisposable()

        mCompositeDisposable_update_Photo?.add(
            ApiRequestClient.createREtrofitInstance()
                .editImageForVehicleRc(  AppConstants.VEHICLEDETAILID,mNumberRc, mPermitValidDate,"true")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::handleResponseUser_Photo, this::handleError_user_photo)
        )
    }


    // handle sucess response of api call registration
    private fun handleResponseUser_Photo(response: ResponseFromServerGeneric) {
        hideDialogLoading()

        if (response.response_code.equals("0")) {
            UtiliyMethods.showDialogwithMessage(activity!!,response.response_message,this )



        }

        mCompositeDisposable_update_Photo?.clear()

    }


    // handle failure response of api call registration
    private fun handleError_user_photo(error: Throwable) {

        hideDialogLoading()
        showSnackBar(error.localizedMessage)
        mCompositeDisposable_update_Photo?.clear()

    }

    private fun validationForAddDriverAdaharPhoto():Boolean {

        mNumberRc = et_rc_no.text.toString()
        mPermitValidDate = et_valid_upto.text.toString()


        if(Validation.isEmptyField(mNumberRc)){
            et_rc_no.error = getString(R.string.error_no_text)
            et_rc_no.requestFocus()
            return true

        }





        return false


    }

    //geeting  number for get individual driver details
    private fun startLoadingForDriverDetails() {


        showDialogLoading()

        mCompositeDisposable_update_Photo = CompositeDisposable()

        mCompositeDisposable_update_Photo?.add(
            ApiRequestClient.createREtrofitInstance()
                .postServerVehicleBasiCDetails(AppConstants.VEHICLEDETAILID)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::handleResponse, this::handleError)
        )
    }

    // handle sucess response of api call of get phone number
    private fun handleResponse(response: ResponseFromServerVehicleDetailList) {
        hideDialogLoading()
        mCompositeDisposable_update_Photo?.clear()


        if(response.vehicle_rc.size>0) {

            et_rc_no.setText(response.vehicle_rc[0].rc_no)


            Glide.with(activity!!).load(response.vehicle_rc[0].rc_photo).into(iv_profile)
        }



    }


    // handle failure response of api call of get phone number
    private fun handleError(error: Throwable) {
        hideDialogLoading()
        showSnackBar(error.localizedMessage)
        mCompositeDisposable_update_Photo?.clear()


    }
    // api call for user registration
    private fun editingUserPhoto() {

        showDialogLoading()
        val file = FileHelper.reduceFileSize(mFileUserPhoto!!)
        val reqFile: RequestBody = RequestBody.create(MediaType.parse("image/*"), file!!)
        val body: MultipartBody.Part = MultipartBody.Part.createFormData("photo", file!!.name.toString(), reqFile)
        val id: RequestBody = RequestBody.create(MediaType.parse("text/plain"), AppConstants.VEHICLEDETAILID)


        mCompositeDisposable_update_Photo = CompositeDisposable()

        mCompositeDisposable_update_Photo?.add(
            ApiRequestClient.createREtrofitInstance()
                .editImageVehicleRc(body, id)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::handleResponseEdit_Photo, this::handleError_Edit_photo)
        )
    }


    // handle sucess response of api call registration
    private fun handleResponseEdit_Photo(response: ResponseFromServerGeneric) {
        hideDialogLoading()
        showDialogWithDismiss(response.response_message)
        mCompositeDisposable_update_Photo?.clear()

    }


    // handle failure response of api call registration
    private fun handleError_Edit_photo(error: Throwable) {

        hideDialogLoading()
        showSnackBar(error.localizedMessage)
        mCompositeDisposable_update_Photo?.clear()

    }

    private fun updateDateInView() {
        val myFormat = "yyyy-MM-dd" // mention the format you need
        val sdf = SimpleDateFormat(myFormat,Locale.US)
        et_valid_upto.setText(sdf.format(cal.time))
        //mPermitValidDate=sdf.format(cal.time)
    }



}
