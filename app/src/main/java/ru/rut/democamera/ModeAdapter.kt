import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import ru.rut.democamera.R

class ModeAdapter(
    private val onPhotoClick: () -> Unit,
    private val onVideoClick: () -> Unit
) : RecyclerView.Adapter<ModeAdapter.BaseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            PHOTO_MODE -> PhotoViewHolder(
                inflater.inflate(R.layout.item_photo_mode, parent, false)
            )
            VIDEO_MODE -> VideoViewHolder(
                inflater.inflate(R.layout.item_video_mode, parent, false)
            )
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        when (holder) {
            is PhotoViewHolder -> holder.bind(onPhotoClick)
            is VideoViewHolder -> holder.bind(onVideoClick)
        }
    }

    override fun getItemCount(): Int = 2

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) PHOTO_MODE else VIDEO_MODE
    }

    abstract class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class PhotoViewHolder(itemView: View) : BaseViewHolder(itemView) {
        private val photoButton: ImageView = itemView.findViewById(R.id.img_capture_btn)

        fun bind(onClick: () -> Unit) {
            photoButton.setOnClickListener { onClick() }
        }
    }

    class VideoViewHolder(itemView: View) : BaseViewHolder(itemView) {
        private val videoButton: ImageView = itemView.findViewById(R.id.video_record_btn)

        fun bind(onClick: () -> Unit) {
            videoButton.setOnClickListener { onClick() }
        }
    }

    companion object {
        private const val PHOTO_MODE = 0
        private const val VIDEO_MODE = 1
    }
}
