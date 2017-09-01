precision highp float;

varying vec4 v_Color;
uniform sampler2D u_Texture;
varying vec2 v_TexCoordinate;

uniform float u_GlobalTime;

vec2 tile_num = vec2(50.0,50.0);//横竖分割

//噪点函数
float snoise(vec2 co){
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
	vec2 uv = fragCoord.xy;
	vec2 uv2 = floor(uv*tile_num)/tile_num;//floor向下取整，平均分，相当于把像素变大
    uv -= uv2;
    uv *= tile_num;
    vec2 delta = vec2(step(1.0-uv.y,uv.x)/(2.0*tile_num.x),step(uv.x,uv.y)/(2.0*tile_num.y));
	fragColor = texture2D( u_Texture, uv2+delta);
}

void main() {
//	mainImage(gl_FragColor, v_TexCoordinate);
    gl_FragColor =texture2D(u_Texture, v_TexCoordinate);//原图
//    gl_FragColor =texture2D(u_Texture, v_TexCoordinate)*snoise(vec2(v_TexCoordinate.x*u_GlobalTime,v_TexCoordinate.y*u_GlobalTime));//噪点动态
//      gl_FragColor =texture2D(u_Texture, v_TexCoordinate)*snoise(v_TexCoordinate);//噪点
//    gl_FragColor = v_Color;//渲染颜色图
}